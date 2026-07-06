package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class AuthMeResponse(

    val userId: String,
    val orgId: String,
    val email: String,
    val role: String,
    val scope: String,
    val schema: String
)