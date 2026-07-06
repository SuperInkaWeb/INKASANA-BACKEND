package com.healthmarketplace.backend.modules.tenant.marketplace.dto

import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import java.math.BigDecimal
import java.util.UUID

data class CreateMarketplaceProfileRequest(
    val profileType: MarketplaceProfileType,
    val doctorId: UUID?,
    val organizationId: UUID?,
    val displayName: String,
    val slug: String,
    val headline: String?,
    val description: String?,
    val city: String?,
    val country: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val consultationPrice: BigDecimal?,
    val consultationDurationMinutes: Int?
)