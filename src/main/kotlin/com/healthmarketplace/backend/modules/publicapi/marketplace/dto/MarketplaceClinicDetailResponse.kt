package com.healthmarketplace.backend.modules.publicapi.marketplace.dto

import java.util.UUID

data class MarketplaceClinicDetailResponse(
    val id: UUID,
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
    val coverImageUrl: String?
)