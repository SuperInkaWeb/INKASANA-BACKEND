package com.healthmarketplace.backend.modules.billing.dto

import jakarta.validation.constraints.Pattern
import java.time.LocalDateTime

data class CreateCheckoutSessionRequest(
    @field:Pattern(
        regexp = "STARTER|PROFESSIONAL|ENTERPRISE",
        message = "El plan seleccionado no es válido"
    )
    val planCode: String
)

data class RedirectUrlResponse(
    val url: String
)

data class BillingSummaryResponse(
    val status: String,
    val planName: String?,
    val currentPeriodEnd: LocalDateTime?,
    val cancelAtPeriodEnd: Boolean
)