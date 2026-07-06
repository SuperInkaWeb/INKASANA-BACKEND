package com.healthmarketplace.backend.modules.tenant.user.dto

import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole

data class CreateTenantUserRequest(
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: TenantUserRole
)