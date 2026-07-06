package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class TenantLoginRequest(
    val slug: String,
    val email: String
)