package com.healthmarketplace.backend.modules.tenant.branding.dto

import java.time.LocalDateTime
import java.util.UUID

data class TenantBrandingResponse(

    val id: UUID?,

    val clinicName: String,

    val slogan: String?,

    val primaryColor: String,

    val secondaryColor: String,

    val logoUrl: String?,

    val faviconUrl: String?,

    val contactEmail: String?,

    val contactPhone: String?,

    val address: String?,

    val city: String?,

    val country: String?,

    val onboardingCompleted: Boolean,

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime
)