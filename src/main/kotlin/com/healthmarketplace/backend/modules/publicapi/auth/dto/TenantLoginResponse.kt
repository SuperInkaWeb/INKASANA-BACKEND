package com.healthmarketplace.backend.modules.publicapi.auth.dto

import java.util.UUID

data class TenantLoginResponse(
    val organizationId: UUID?,
    val organizationName: String,
    val slug: String,
    val schemaName: String,
    val userId: UUID?,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String
)