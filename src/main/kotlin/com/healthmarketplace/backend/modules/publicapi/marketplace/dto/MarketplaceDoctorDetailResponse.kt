package com.healthmarketplace.backend.modules.publicapi.marketplace.dto

import java.math.BigDecimal
import java.util.UUID

data class MarketplaceDoctorDetailResponse(
    val id: UUID,
    val doctorId: UUID?,
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
    val consultationDurationMinutes: Int?,
    val availableDays: List<String>,
    val availableStartTime: String?,
    val availableEndTime: String?,
    val specialties: List<String> = emptyList()
)