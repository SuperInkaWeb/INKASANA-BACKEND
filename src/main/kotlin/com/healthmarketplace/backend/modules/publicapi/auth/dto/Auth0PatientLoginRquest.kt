package com.healthmarketplace.backend.modules.publicapi.auth.dto

data class Auth0PatientLoginRequest(
    val email: String,
    val auth0Id: String
)