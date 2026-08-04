package com.healthmarketplace.backend.modules.tenant.marketplace.dto

import java.math.BigDecimal

data class UpdateMarketplaceProfileRequest(
    val displayName: String?,
    val headline: String?,
    val description: String?,
    val city: String?,
    val country: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val carouselImageUrl1: String?,
    val carouselImageUrl2: String?,
    val pageColor: String?,
    val buttonColor: String?,
    val subscriptionColor: String?,
    val appearanceConfig: String?,
    val consultationPrice: BigDecimal?,
    val consultationDurationMinutes: Int?
)
