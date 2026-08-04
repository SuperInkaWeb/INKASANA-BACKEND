package com.healthmarketplace.backend.modules.billing.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.mercadopago")
data class MercadoPagoProperties(
    val accessToken: String,
    val webhookSecret: String,
    val frontendUrl: String,
    val currency: String = "PEN"
)