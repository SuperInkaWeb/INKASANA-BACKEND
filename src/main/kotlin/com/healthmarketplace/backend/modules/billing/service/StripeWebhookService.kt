package com.healthmarketplace.backend.modules.billing.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StripeWebhookService(private val jdbcTemplate: JdbcTemplate, private val objectMapper: ObjectMapper, private val stripeProperties: StripeProperties) {
    @Transactional
    fun process(payload: String, signature: String) {
        require(stripeProperties.webhookSecret.isNotBlank()) { "STRIPE_WEBHOOK_SECRET es obligatoria para recibir webhooks" }
        val event = Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret)
        val inserted = jdbcTemplate.update("INSERT INTO stripe_webhook_events (stripe_event_id, event_type) VALUES (?, ?) ON CONFLICT (stripe_event_id) DO NOTHING", event.id, event.type)
        if (inserted == 0 || event.type != "checkout.session.completed") return
        val session = objectMapper.readTree(payload).path("data").path("object")
        if (session.path("mode").asText() != "subscription") return
        val organizationId = session.path("metadata").path("organizationId").asText()
        val planCode = session.path("metadata").path("planCode").asText()
        val subscriptionId = session.path("subscription").asText()
        val customerId = session.path("customer").asText()
        if (organizationId.isBlank() || planCode.isBlank() || subscriptionId.isBlank()) return
        val subscription = Subscription.retrieve(subscriptionId)
        jdbcTemplate.update("UPDATE subscriptions SET plan_id = (SELECT id FROM plans WHERE code = ?), status = ?, stripe_customer_id = ?, stripe_subscription_id = ?, cancel_at_period_end = ?, updated_at = NOW() WHERE organization_id = ?", planCode, subscription.status.uppercase(), customerId, subscriptionId, subscription.cancelAtPeriodEnd, UUID.fromString(organizationId))
    }
}
