package com.healthmarketplace.backend.modules.publicapi.doctorregistration.dto

data class RegisterIndependentDoctorRequest(
    val fullName: String,
    val email: String,
    val phone: String?,
    val specialty: String?,
    val address: String?,
    val city: String?,
    val country: String?,
    val professionalName: String?
)