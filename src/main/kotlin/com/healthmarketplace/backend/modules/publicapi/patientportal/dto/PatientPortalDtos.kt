package com.healthmarketplace.backend.modules.publicapi.patientportal.dto

import java.time.LocalDate
import java.time.LocalTime

data class UpdatePatientPortalProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val dni: String?,
    val phone: String?
)

data class PatientPortalProfileResponse(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val dni: String?,
    val phone: String?,
    val avatarUrl: String?
)

data class PatientPortalAppointmentResponse(
    val id: String,
    val doctorName: String,
    val clinicName: String?,
    val date: LocalDate,
    val time: LocalTime,
    val status: String,
    val reason: String? = null
)
