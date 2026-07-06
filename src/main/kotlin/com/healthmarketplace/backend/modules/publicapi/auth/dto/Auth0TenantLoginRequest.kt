package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class Auth0TenantLoginRequest(
    val slug: String,
    val email: String,
    val auth0Id: String
)