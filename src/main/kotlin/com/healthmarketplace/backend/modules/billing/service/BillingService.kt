package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class BillingService(
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val mercadoPagoClient: MercadoPagoClient,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    fun summary(organizationId: UUID): BillingSummaryResponse {
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
        ).firstOrNull() ?: throw IllegalStateException("La organizacion no tiene una suscripcion activa")
        mercadoPagoClient.put("/preapproval/$subscriptionId", mapOf("status" to "cancelled"))
        jdbcTemplate.update(
            "UPDATE subscriptions SET status = 'CANCELED', cancel_at_period_end = TRUE, updated_at = NOW() WHERE mercadopago_preapproval_id = ?",
            subscriptionId
        )
        return summary(organizationId)
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
