package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest
import com.mercadopago.client.preapproval.PreapprovalClient
import com.mercadopago.client.preapproval.PreapprovalCreateRequest
import com.mercadopago.client.preapproval.PreapprovalUpdateRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class BillingService(
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    private val preapprovalClient = PreapprovalClient()

    fun summary(organizationId: UUID): BillingSummaryResponse {
        val rows = jdbcTemplate.query(
            """
            SELECT s.status, p.name, s.current_period_end, s.cancel_at_period_end
            FROM subscriptions s
            JOIN plans p ON p.id = s.plan_id
            WHERE s.organization_id = ?
            ORDER BY s.updated_at DESC
            LIMIT 1
            """.trimIndent(),
            { rs, _ ->
                BillingSummaryResponse(
                    status = rs.getString("status"),
                    planName = rs.getString("name"),
                    currentPeriodEnd = rs.getTimestamp("current_period_end")?.toLocalDateTime(),
                    cancelAtPeriodEnd = rs.getBoolean("cancel_at_period_end")
                )
            },
            organizationId
        )

        return rows.firstOrNull()
            ?: BillingSummaryResponse(
                status = "NONE",
                planName = null,
                currentPeriodEnd = null,
                cancelAtPeriodEnd = false
            )
    }

    @Transactional
    fun checkout(organizationId: UUID, planCode: String): RedirectUrlResponse {
        val plan = jdbcTemplate.query(
            """
            SELECT id, name, price_cents
            FROM plans
            WHERE code = ? AND is_active = TRUE
            """.trimIndent(),
            { rs, _ ->
                Plan(
                    id = UUID.fromString(rs.getString("id")),
                    name = rs.getString("name"),
                    priceCents = rs.getLong("price_cents")
                )
            },
            planCode
        ).firstOrNull() ?: throw IllegalArgumentException("El plan no existe o no está disponible")

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalArgumentException("Organización no encontrada") }

        require(!organization.email.isNullOrBlank()) {
            "La organización no tiene un correo configurado para facturación"
        }

        val amount = BigDecimal.valueOf(plan.priceCents).divide(BigDecimal(100))

        val createRequest = PreapprovalCreateRequest.builder()
            .reason(plan.name)
            .externalReference("$organizationId:$planCode")
            .payerEmail(organization.email)
            .backUrl("${mercadoPagoProperties.frontendUrl}/billing?checkout=success")
            .autoRecurring(
                PreApprovalAutoRecurringCreateRequest.builder()
                    .frequency(1)
                    .frequencyType("months")
                    .transactionAmount(amount)
                    .currencyId(mercadoPagoProperties.currency)
                    .build()
            )
            .build()

        val preapproval = try {
            preapprovalClient.create(createRequest)
        } catch (ex: com.mercadopago.exceptions.MPApiException) {
            val body = ex.apiResponse?.content
            org.slf4j.LoggerFactory.getLogger(BillingService::class.java)
                .error("MercadoPago rechazó el checkout. Status={} Body={}", ex.apiResponse?.statusCode, body)
            throw IllegalStateException("MercadoPago rechazó la solicitud: $body")
        }

        val subscriptionsCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subscriptions WHERE organization_id = ?",
            Int::class.java,
            organizationId
        ) ?: 0

        if (subscriptionsCount == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO subscriptions (
                    organization_id, plan_id, status,
                    mercadopago_preapproval_id, mercadopago_payer_email
                ) VALUES (?, ?, 'INCOMPLETE', ?, ?)
                """.trimIndent(),
                organizationId, plan.id, preapproval.id, organization.email
            )
        } else {
            jdbcTemplate.update(
                """
                UPDATE subscriptions
                SET plan_id = ?,
                    mercadopago_preapproval_id = ?,
                    mercadopago_payer_email = ?,
                    status = 'INCOMPLETE',
                    updated_at = NOW()
                WHERE id = (
                    SELECT id FROM subscriptions
                    WHERE organization_id = ?
                    ORDER BY updated_at DESC
                    LIMIT 1
                )
                """.trimIndent(),
                plan.id, preapproval.id, organization.email, organizationId
            )
        }

        return RedirectUrlResponse(url = requireNotNull(preapproval.initPoint))
    }

    @Transactional
    fun cancel(organizationId: UUID): BillingSummaryResponse {
        val preapprovalId = jdbcTemplate.query(
            """
            SELECT mercadopago_preapproval_id
            FROM subscriptions
            WHERE organization_id = ?
              AND mercadopago_preapproval_id IS NOT NULL
            ORDER BY updated_at DESC
            LIMIT 1
            """.trimIndent(),
            { rs, _ -> rs.getString("mercadopago_preapproval_id") },
            organizationId
        ).firstOrNull() ?: throw IllegalStateException("La organización no tiene una suscripción activa")

        preapprovalClient.update(
            preapprovalId,
            PreapprovalUpdateRequest.builder().status("cancelled").build()
        )

        jdbcTemplate.update(
            """
            UPDATE subscriptions
            SET status = 'CANCELED',
                cancel_at_period_end = TRUE,
                updated_at = NOW()
            WHERE mercadopago_preapproval_id = ?
            """.trimIndent(),
            preapprovalId
        )

        return summary(organizationId)
    }

    private data class Plan(
        val id: UUID,
        val name: String,
        val priceCents: Long
    )
}