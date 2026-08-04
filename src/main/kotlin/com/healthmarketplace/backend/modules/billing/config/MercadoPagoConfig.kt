package com.healthmarketplace.backend.modules.billing.config

import com.mercadopago.MercadoPagoConfig as MpSdkConfig
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class MercadoPagoConfig(
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    @PostConstruct
    fun configure() {
        MpSdkConfig.setAccessToken(mercadoPagoProperties.accessToken)
    }
}