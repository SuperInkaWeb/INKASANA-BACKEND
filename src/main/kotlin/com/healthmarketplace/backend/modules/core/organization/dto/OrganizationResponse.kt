package com.healthmarketplace.backend.modules.core.organization.dto

import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationType
import java.time.LocalDateTime
import java.util.UUID

data class OrganizationResponse(
    val id: UUID?,
    val name: String,
    val slug: String,
    val schemaName: String,
    val type: OrganizationType,
    val status: OrganizationStatus,
    val email: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val country: String?,
    val schemaReady: Boolean,
    val schemaReadyAt: LocalDateTime?,
    val provisioningError: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)