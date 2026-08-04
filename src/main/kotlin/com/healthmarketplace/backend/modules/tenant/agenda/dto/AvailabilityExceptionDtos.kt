package com.healthmarketplace.backend.modules.tenant.agenda.dto

import com.healthmarketplace.backend.modules.tenant.agenda.model.AvailabilityExceptionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class CreateAvailabilityExceptionRequest(
    val exceptionDate: LocalDate,
    val type: AvailabilityExceptionType,
    // Obligatorios solo si type = EXTRA. Si type = UNAVAILABLE deben venir null.
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val reason: String? = null
)

data class UpdateAvailabilityExceptionRequest(
    val exceptionDate: LocalDate? = null,
    val type: AvailabilityExceptionType? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val reason: String? = null
)

data class AvailabilityExceptionResponse(
    val id: UUID,
    val doctorId: UUID,
    val exceptionDate: LocalDate,
    val type: AvailabilityExceptionType,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val reason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
