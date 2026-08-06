package com.healthmarketplace.backend.modules.billing.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.stripe")
data class StripeProperties(
    val secretKey: String,
    val webhookSecret: String = "",
    val frontendUrl: String,
    val currency: String = "pen"
)
