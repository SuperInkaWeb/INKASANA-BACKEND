package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException
import com.mercadopago.webhook.WebhookSignatureValidator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MercadoPagoWebhookService(
    private val jdbcTemplate: JdbcTemplate,
    private val mercadoPagoClient: MercadoPagoClient,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    @Transactional
    fun process(xSignature: String?, xRequestId: String?, dataId: String?, type: String?) {
        require(mercadoPagoProperties.webhookSecret.isNotBlank()) { "MERCADOPAGO_WEBHOOK_SECRET es obligatoria para recibir webhooks" }
        require(!xSignature.isNullOrBlank() && !xRequestId.isNullOrBlank() && !dataId.isNullOrBlank()) { "Webhook de Mercado Pago incompleto" }
        try {
            WebhookSignatureValidator.validate(xSignature, xRequestId, dataId, mercadoPagoProperties.webhookSecret)
        } catch (exception: MPInvalidWebhookSignatureException) {
            throw IllegalArgumentException("Firma de webhook de Mercado Pago invalida", exception)
        }
        val inserted = jdbcTemplate.update(
            "INSERT INTO mercadopago_webhook_events (mercadopago_event_id, event_type) VALUES (?, ?) ON CONFLICT (mercadopago_event_id) DO NOTHING",
            xRequestId, type ?: "unknown"
        )
        if (inserted == 0 || type != "subscription_preapproval") return
        synchronizeSubscription(dataId)
    }

    private fun synchronizeSubscription(preapprovalId: String) {
        val subscription = mercadoPagoClient.get("/preapproval/$preapprovalId")
        val organizationId = subscription.path("external_reference").asText().let { reference ->
            runCatching { UUID.fromString(reference) }.getOrNull()
        } ?: return
        val status = when (subscription.path("status").asText().lowercase()) {
            "authorized" -> "ACTIVE"
            "cancelled" -> "CANCELED"
            "paused" -> "PAST_DUE"
            else -> "INCOMPLETE"
        }
        val nextPaymentDate = subscription.path("next_payment_date").asText().takeIf { it.isNotBlank() }
            ?.let { OffsetDateTime.parse(it).toLocalDateTime() }
        jdbcTemplate.update(
            """
            UPDATE subscriptions
            SET status = ?, mercadopago_preapproval_id = ?, mercadopago_payer_id = NULLIF(?, ''),
                mercadopago_payer_email = NULLIF(?, ''), current_period_end = ?,
                cancel_at_period_end = ?, updated_at = NOW()
            WHERE organization_id = ?
            """.trimIndent(),
            status,
            preapprovalId,
            subscription.path("payer_id").asText(),
            subscription.path("payer_email").asText(),
            nextPaymentDate,
            status == "CANCELED",
            organizationId
        )
    }
}
