package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class TenantLoginTokenResponse(

    val accessToken: String,

    val tokenType: String = "Bearer",

    val expiresIn: Long,

    val user: TenantUserSessionDto,

    val organization: TenantOrganizationSessionDto
)

data class TenantUserSessionDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String
)

data class TenantOrganizationSessionDto(
    val id: String,
    val name: String,
    val slug: String,
    val schemaName: String
)