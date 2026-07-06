package com.healthmarketplace.backend.modules.tenant.patient.dto

import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PatientResponse(
    val id: UUID,
    val fullName: String,
    val identification: String?,
    val birthDate: LocalDate?,
    val gender: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val status: PatientStatus,
    val emergencyContactName: String?,
    val emergencyContactPhone: String?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)