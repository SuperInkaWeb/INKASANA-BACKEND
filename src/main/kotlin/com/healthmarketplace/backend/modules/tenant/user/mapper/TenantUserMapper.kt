package com.healthmarketplace.backend.modules.tenant.user.mapper

import com.healthmarketplace.backend.modules.tenant.user.dto.TenantUserResponse
import com.healthmarketplace.backend.modules.tenant.user.entity.TenantUser

fun TenantUser.toResponse(): TenantUserResponse {
    return TenantUserResponse(
        id = this.id!!,
        auth0Id = this.auth0Id,
        email = this.email,
        fullName = this.fullName,
        phone = this.phone,
        role = this.role,
        status = this.status,
        profileImageUrl = this.profileImageUrl,
        lastLogin = this.lastLogin,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}