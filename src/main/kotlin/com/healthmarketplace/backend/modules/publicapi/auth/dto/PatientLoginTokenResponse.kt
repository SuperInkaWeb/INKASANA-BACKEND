package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class PatientLoginTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 3600,
    val user: PatientLoginUserResponse
)

data class PatientLoginUserResponse(
    val id: String,
    val email: String,
    val role: String = "PATIENT"
)