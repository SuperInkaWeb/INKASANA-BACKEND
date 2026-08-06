package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.stripe.model.Customer
import com.stripe.model.Subscription
import com.stripe.model.checkout.Session
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionUpdateParams
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BillingService(
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val stripeProperties: StripeProperties
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
        ).firstOrNull() ?: throw IllegalArgumentException("El plan no existe o no está disponible")
        require(plan.priceCents > 0) { "El plan Esencial es gratuito y no requiere pago." }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalArgumentException("Organización no encontrada") }
        val customerId = findOrCreateCustomer(organizationId, organization.email, organization.name)
        saveIncompleteSubscription(organizationId, plan.id, customerId)

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setClientReferenceId(organizationId.toString())
            .setSuccessUrl("${stripeProperties.frontendUrl}/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("${stripeProperties.frontendUrl}/billing?checkout=cancelled")
            .putMetadata("organizationId", organizationId.toString())
            .putMetadata("planCode", planCode)
            .addLineItem(
                SessionCreateParams.LineItem.builder().setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(stripeProperties.currency)
                            .setUnitAmount(plan.priceCents)
                            .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder().setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH).build())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(plan.name).build())
                            .build()
                    ).build()
            ).build()
        return RedirectUrlResponse(Session.create(params).url)
    }

    @Transactional
    fun cancel(organizationId: UUID): BillingSummaryResponse {
        val subscriptionId = jdbcTemplate.query(
            "SELECT stripe_subscription_id FROM subscriptions WHERE organization_id = ? AND stripe_subscription_id IS NOT NULL ORDER BY updated_at DESC LIMIT 1",
            { rs, _ -> rs.getString("stripe_subscription_id") }, organizationId
        ).firstOrNull() ?: throw IllegalStateException("La organización no tiene una suscripción activa")
        Subscription.retrieve(subscriptionId).update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build())
        jdbcTemplate.update("UPDATE subscriptions SET cancel_at_period_end = TRUE, updated_at = NOW() WHERE stripe_subscription_id = ?", subscriptionId)
        return summary(organizationId)
    }

    private fun findOrCreateCustomer(organizationId: UUID, email: String?, name: String): String {
        val existing = jdbcTemplate.query(
            "SELECT stripe_customer_id FROM subscriptions WHERE organization_id = ? AND stripe_customer_id IS NOT NULL ORDER BY updated_at DESC LIMIT 1",
            { rs, _ -> rs.getString("stripe_customer_id") }, organizationId
        ).firstOrNull()
        if (existing != null) return existing
        val params = CustomerCreateParams.builder().setName(name).putMetadata("organizationId", organizationId.toString()).apply { if (!email.isNullOrBlank()) setEmail(email) }.build()
        return Customer.create(params).id
    }

    private fun saveIncompleteSubscription(organizationId: UUID, planId: UUID, customerId: String) {
        val exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE organization_id = ?", Int::class.java, organizationId) ?: 0
        if (exists == 0) jdbcTemplate.update("INSERT INTO subscriptions (organization_id, plan_id, status, stripe_customer_id) VALUES (?, ?, 'INCOMPLETE', ?)", organizationId, planId, customerId)
        else jdbcTemplate.update("UPDATE subscriptions SET plan_id = ?, stripe_customer_id = ?, stripe_subscription_id = NULL, status = 'INCOMPLETE', cancel_at_period_end = FALSE, updated_at = NOW() WHERE id = (SELECT id FROM subscriptions WHERE organization_id = ? ORDER BY updated_at DESC LIMIT 1)", planId, customerId, organizationId)
    }

    private data class Plan(val id: UUID, val name: String, val priceCents: Long)
}
