package com.healthmarketplace.backend.modules.publicapi.marketplace.dto

import java.util.UUID

data class MarketplaceClinicSearchResponse(
    val id: UUID,
    val organizationId: UUID?,
    val displayName: String,
    val slug: String,
    val headline: String?,
    val city: String?,
    val country: String?,
    val address: String?,
    val phone: String?,
    val profileImageUrl: String?
)