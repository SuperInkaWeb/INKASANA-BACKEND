package com.healthmarketplace.backend.modules.tenant.user.dto

import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import java.time.LocalDateTime
import java.util.UUID

data class TenantUserResponse(
    val id: UUID,
    val auth0Id: String?,
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: TenantUserRole,
    val status: TenantUserStatus,
    val profileImageUrl: String?,
    val lastLogin: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)