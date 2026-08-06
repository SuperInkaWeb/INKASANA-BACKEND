package com.healthmarketplace.backend.modules.billing.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig(private val stripeProperties: StripeProperties) {
    @PostConstruct
    fun configure() {
        require(stripeProperties.secretKey.isNotBlank()) { "STRIPE_SECRET_KEY es obligatoria" }
        Stripe.apiKey = stripeProperties.secretKey
    }
}
