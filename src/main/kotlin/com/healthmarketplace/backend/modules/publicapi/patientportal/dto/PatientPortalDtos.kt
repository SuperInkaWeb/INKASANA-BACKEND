package com.healthmarketplace.backend.modules.publicapi.patientportal.dto

data class UpdatePatientPortalProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val dni: String?
)

data class PatientPortalProfileResponse(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val dni: String?,
    val avatarUrl: String?
)