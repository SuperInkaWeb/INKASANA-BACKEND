package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.billing.dto.PaymentHistoryItemResponse
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BillingService(
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val mercadoPagoClient: MercadoPagoClient,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    fun paymentHistory(organizationId: UUID): List<PaymentHistoryItemResponse> {
        synchronizeFromMercadoPago(organizationId)
        return jdbcTemplate.query(
            """
            SELECT p.id, p.purpose, i.invoice_number, p.amount_cents, p.currency, p.status, p.paid_at
            FROM payments p
            LEFT JOIN invoices i ON i.organization_id = p.organization_id
              AND i.stripe_invoice_id = CONCAT('mercadopago-payment-', p.mercadopago_payment_id)
            WHERE p.organization_id = ?
            ORDER BY COALESCE(p.paid_at, p.created_at) DESC
            """.trimIndent(),
            { rs, _ -> PaymentHistoryItemResponse(
                rs.getObject("id", UUID::class.java).toString(), rs.getString("purpose"), rs.getString("invoice_number"),
                rs.getLong("amount_cents"), rs.getString("currency"), rs.getString("status"),
                rs.getTimestamp("paid_at")?.toLocalDateTime()
            ) }, organizationId
        )
    }

    fun summary(organizationId: UUID): BillingSummaryResponse {
        synchronizeFromMercadoPago(organizationId)
        val rows = jdbcTemplate.query(
            """
            SELECT s.status, p.name, s.current_period_end, s.cancel_at_period_end
            FROM subscriptions s JOIN plans p ON p.id = s.plan_id
            WHERE s.organization_id = ? ORDER BY s.updated_at DESC LIMIT 1
            """.trimIndent(),
            { rs, _ -> BillingSummaryResponse(rs.getString("status"), rs.getString("name"), rs.getTimestamp("current_period_end")?.toLocalDateTime(), rs.getBoolean("cancel_at_period_end")) },
            organizationId
        )
        return rows.firstOrNull() ?: BillingSummaryResponse("NONE", null, null, false)
    }

    /**
     * Los webhooks son la vía normal, pero esta conciliación evita que una
     * notificación perdida deje el pago o la suscripción desactualizados.
     */
    private fun synchronizeFromMercadoPago(organizationId: UUID) {
        val preapprovalId = jdbcTemplate.query(
            "SELECT mercadopago_preapproval_id FROM subscriptions WHERE organization_id = ? AND mercadopago_preapproval_id IS NOT NULL ORDER BY updated_at DESC LIMIT 1",
            { rs, _ -> rs.getString("mercadopago_preapproval_id") }, organizationId
        ).firstOrNull() ?: return

        runCatching {
            val subscription = mercadoPagoClient.get("/preapproval/$preapprovalId")
            val status = when (subscription.path("status").asText().lowercase()) {
                "authorized" -> "ACTIVE"
                "cancelled" -> "CANCELED"
                "paused" -> "PAST_DUE"
                else -> "INCOMPLETE"
            }
            val nextPaymentDate = subscription.path("next_payment_date").asText().takeIf { it.isNotBlank() }
                ?.let { OffsetDateTime.parse(it).toLocalDateTime() }
            jdbcTemplate.update(
                "UPDATE subscriptions SET status = ?, current_period_end = ?, cancel_at_period_end = ?, updated_at = NOW() WHERE organization_id = ? AND mercadopago_preapproval_id = ?",
                status, nextPaymentDate, status == "CANCELED", organizationId, preapprovalId
            )

            val payments = mercadoPagoClient.get("/v1/payments/search?preapproval_id=$preapprovalId&status=approved&sort=date_created&criteria=desc&limit=20")
            payments.path("results").filter { it.path("status").asText().lowercase() == "approved" }.forEach { payment ->
                val paymentId = payment.path("id").asText().takeIf { it.isNotBlank() } ?: return@forEach
                val amountCents = payment.path("transaction_amount").decimalValue()
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
                val currency = payment.path("currency_id").asText().ifBlank { mercadoPagoProperties.currency.uppercase() }
                val paidAt = payment.path("date_approved").asText().takeIf { it.isNotBlank() }
                    ?.let { OffsetDateTime.parse(it).toLocalDateTime() } ?: LocalDateTime.now()
                saveSubscriptionPayment(organizationId, paymentId, amountCents, currency, paidAt)
            }
        }.onFailure { exception ->
            // La vista conserva el último estado conocido si Mercado Pago no está disponible.
            println("No se pudo conciliar la suscripción de $organizationId: ${exception.message}")
        }
    }

    private fun saveSubscriptionPayment(
        organizationId: UUID,
        paymentId: String,
        amountCents: Long,
        currency: String,
        paidAt: LocalDateTime
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO payments (organization_id, mercadopago_payment_id, amount_cents, currency, status, paid_at, purpose)
            VALUES (?, ?, ?, ?, 'PAID', ?, 'SUBSCRIPTION')
            ON CONFLICT (mercadopago_payment_id) DO UPDATE
            SET status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(), organizationId, paymentId, amountCents, currency, paidAt
        )
        jdbcTemplate.update(
            """
            INSERT INTO invoices (organization_id, stripe_invoice_id, invoice_number, amount_due_cents, amount_paid_cents, currency, status, paid_at)
            VALUES (?, ?, ?, ?, ?, ?, 'PAID', ?)
            ON CONFLICT (stripe_invoice_id) DO UPDATE
            SET amount_paid_cents = EXCLUDED.amount_paid_cents, status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(), organizationId, "mercadopago-payment-$paymentId", "MP-$paymentId", amountCents, amountCents, currency, paidAt
        )
    }

    @Transactional
    fun checkout(organizationId: UUID, planCode: String): RedirectUrlResponse {
        val plan = jdbcTemplate.query(
            "SELECT id, name, price_cents FROM plans WHERE code = ? AND is_active = TRUE",
            { rs, _ -> Plan(UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getLong("price_cents")) },
            planCode
        ).firstOrNull() ?: throw IllegalArgumentException("El plan no existe o no esta disponible")
        require(plan.priceCents > 0) { "El plan Esencial es gratuito y no requiere pago." }
        require(mercadoPagoProperties.webhookUrl.isNotBlank()) { "MERCADOPAGO_WEBHOOK_URL es obligatoria para iniciar el checkout" }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalArgumentException("Organizacion no encontrada") }
        val payerEmail = mercadoPagoProperties.testPayerEmail.takeIf { it.isNotBlank() }
            ?: organization.email?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("La organizacion necesita un correo para suscribirse")

        // Si el usuario abandonó un checkout anterior, se cancela antes de
        // crear el nuevo para no dejar preaprovals pendientes en Mercado Pago.
        cancelPendingSubscription(organizationId)

        val response = mercadoPagoClient.post(
            "/preapproval",
            mapOf(
                "reason" to "Plan ${plan.name}",
                "external_reference" to organizationId.toString(),
                "payer_email" to payerEmail,
                "auto_recurring" to mapOf(
                    "frequency" to 1,
                    "frequency_type" to "months",
                    "transaction_amount" to BigDecimal.valueOf(plan.priceCents, 2),
                    "currency_id" to mercadoPagoProperties.currency.uppercase()
                ),
                "back_url" to "${mercadoPagoProperties.frontendUrl}/billing?checkout=success",
                "notification_url" to mercadoPagoProperties.webhookUrl
            )
        )
        val preapprovalId = response.path("id").asText()
        val checkoutUrl = response.path("sandbox_init_point").asText().ifBlank { response.path("init_point").asText() }
        require(preapprovalId.isNotBlank() && checkoutUrl.isNotBlank()) { "Mercado Pago no devolvio el enlace de checkout" }
        saveIncompleteSubscription(organizationId, plan.id, preapprovalId, payerEmail)
        return RedirectUrlResponse(checkoutUrl)
    }

    @Transactional
    fun cancel(organizationId: UUID): BillingSummaryResponse {
        val subscriptionId = jdbcTemplate.query(
            "SELECT mercadopago_preapproval_id FROM subscriptions WHERE organization_id = ? AND mercadopago_preapproval_id IS NOT NULL ORDER BY updated_at DESC LIMIT 1",
            { rs, _ -> rs.getString("mercadopago_preapproval_id") }, organizationId
        ).firstOrNull() ?: throw IllegalStateException("La organizacion no tiene una suscripcion pendiente o activa")
        mercadoPagoClient.put("/preapproval/$subscriptionId", mapOf("status" to "cancelled"))
        jdbcTemplate.update(
            "UPDATE subscriptions SET status = 'CANCELED', cancel_at_period_end = TRUE, updated_at = NOW() WHERE mercadopago_preapproval_id = ?",
            subscriptionId
        )
        return summary(organizationId)
    }

    private fun cancelPendingSubscription(organizationId: UUID) {
        val pendingId = jdbcTemplate.query(
            """
            SELECT mercadopago_preapproval_id FROM subscriptions
            WHERE organization_id = ? AND status IN ('INCOMPLETE', 'PAST_DUE')
              AND mercadopago_preapproval_id IS NOT NULL
            ORDER BY updated_at DESC LIMIT 1
            """.trimIndent(),
            { rs, _ -> rs.getString("mercadopago_preapproval_id") }, organizationId
        ).firstOrNull() ?: return
        mercadoPagoClient.put("/preapproval/$pendingId", mapOf("status" to "cancelled"))
        jdbcTemplate.update(
            "UPDATE subscriptions SET status = 'CANCELED', cancel_at_period_end = TRUE, updated_at = NOW() WHERE mercadopago_preapproval_id = ?",
            pendingId
        )
    }

    private fun saveIncompleteSubscription(organizationId: UUID, planId: UUID, preapprovalId: String, payerEmail: String) {
        val exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE organization_id = ?", Int::class.java, organizationId) ?: 0
        if (exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO subscriptions (organization_id, plan_id, status, mercadopago_preapproval_id, mercadopago_payer_email) VALUES (?, ?, 'INCOMPLETE', ?, ?)",
                organizationId, planId, preapprovalId, payerEmail
            )
        } else {
            jdbcTemplate.update(
                "UPDATE subscriptions SET plan_id = ?, mercadopago_preapproval_id = ?, mercadopago_payer_email = ?, status = 'INCOMPLETE', cancel_at_period_end = FALSE, updated_at = NOW() WHERE id = (SELECT id FROM subscriptions WHERE organization_id = ? ORDER BY updated_at DESC LIMIT 1)",
                planId, preapprovalId, payerEmail, organizationId
            )
        }
    }

    private data class Plan(val id: UUID, val name: String, val priceCents: Long)
}
