package com.healthmarketplace.backend.modules.billing.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.mercadopago.client.preapproval.PreapprovalClient
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class MercadoPagoWebhookService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    private val preapprovalClient = PreapprovalClient()
    private val httpClient = HttpClient.newHttpClient()

    @Transactional
    fun process(
        payload: String,
        signature: String,
        requestId: String,
        dataIdParam: String?
    ) {
        val body = objectMapper.readTree(payload)
        val type = body.path("type").asText()
        val dataId = dataIdParam ?: body.path("data").path("id").asText()

        require(dataId.isNotBlank()) { "La notificación de Mercado Pago no trae data.id" }

        verifySignature(signature, requestId, dataId)

        val eventId = "$type:$dataId"
        val inserted = jdbcTemplate.update(
            """
            INSERT INTO mercadopago_webhook_events (mercadopago_event_id, event_type)
            VALUES (?, ?)
            ON CONFLICT (mercadopago_event_id) DO NOTHING
            """.trimIndent(),
            eventId,
            type
        )

        if (inserted == 0) return

        when (type) {
            "subscription_preapproval" -> handlePreapproval(dataId)
            "subscription_authorized_payment" -> handleAuthorizedPayment(dataId)
        }
    }

    private fun verifySignature(
        signature: String,
        requestId: String,
        dataId: String
    ) {
        if (mercadoPagoProperties.webhookSecret.isBlank()) return

        val parts = signature.split(",")
            .mapNotNull { chunk ->
                val piece = chunk.split("=", limit = 2)
                if (piece.size == 2) piece[0].trim() to piece[1].trim() else null
            }
            .toMap()

        val ts = parts["ts"]
        val hash = parts["v1"]
        require(ts != null && hash != null) { "Firma de Mercado Pago con formato inválido" }

        val manifest = "id:$dataId;request-id:$requestId;ts:$ts;"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(mercadoPagoProperties.webhookSecret.toByteArray(), "HmacSHA256"))
        val computed = mac.doFinal(manifest.toByteArray())
            .joinToString("") { "%02x".format(it) }

        if (!computed.equals(hash, ignoreCase = true)) {
            throw SecurityException("Firma de Mercado Pago inválida")
        }
    }

    private fun handlePreapproval(preapprovalId: String) {
        val preapproval = preapprovalClient.get(preapprovalId)

        val status = when (preapproval.status) {
            "authorized" -> "ACTIVE"
            "paused" -> "PAST_DUE"
            "cancelled" -> "CANCELED"
            else -> "INCOMPLETE"
        }

        jdbcTemplate.update(
            """
            UPDATE subscriptions
            SET status = ?,
                mercadopago_payer_id = ?,
                updated_at = NOW()
            WHERE mercadopago_preapproval_id = ?
            """.trimIndent(),
            status,
            preapproval.payerId?.toString(),
            preapprovalId
        )
    }

    private fun handleAuthorizedPayment(authorizedPaymentId: String) {
        // El SDK de Java no trae un cliente propio para /authorized_payments,
        // así que se consulta directo por HTTP.
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.mercadopago.com/authorized_payments/$authorizedPaymentId"))
            .header("Authorization", "Bearer ${mercadoPagoProperties.accessToken}")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return

        val payment = objectMapper.readTree(response.body())
        val preapprovalId = payment.path("preapproval_id").asText()

        val subscription = jdbcTemplate.query(
            """
            SELECT id, organization_id
            FROM subscriptions
            WHERE mercadopago_preapproval_id = ?
            """.trimIndent(),
            { rs, _ -> rs.getString("id") to rs.getString("organization_id") },
            preapprovalId
        ).firstOrNull() ?: return

        val (subscriptionId, organizationId) = subscription
        val status = payment.path("status").asText().uppercase()
        val amountCents = Math.round(payment.path("transaction_amount").asDouble() * 100)

        jdbcTemplate.update(
            """
            INSERT INTO payments (
                organization_id, subscription_id, mercadopago_payment_id,
                amount_cents, currency, status, paid_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (mercadopago_payment_id) DO UPDATE
            SET status = EXCLUDED.status,
                paid_at = EXCLUDED.paid_at,
                updated_at = NOW()
            """.trimIndent(),
            UUID.fromString(organizationId),
            UUID.fromString(subscriptionId),
            authorizedPaymentId,
            amountCents,
            payment.path("currency_id").asText(),
            status,
            if (status == "APPROVED" || status == "PROCESSED") Timestamp.from(Instant.now()) else null
        )
    }
}