package com.healthmarketplace.backend.modules.tenant.patient.dto

import java.time.LocalDate

data class UpdatePatientRequest(
    val fullName: String?,
    val identification: String?,
    val birthDate: LocalDate?,
    val gender: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val notes: String?
)