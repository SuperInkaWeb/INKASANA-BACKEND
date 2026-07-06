package com.healthmarketplace.backend.modules.tenant.user.dto

import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole

data class UpdateTenantUserRequest(
    val fullName: String?,
    val phone: String?,
    val role: TenantUserRole?,
    val profileImageUrl: String?
)