package com.healthmarketplace.backend.modules.tenant.agenda.dto

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class CreateDoctorAvailabilityRequest(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean = true
)

// Todos los campos opcionales: se actualiza solo lo que venga en el body.
data class UpdateDoctorAvailabilityRequest(
    val dayOfWeek: DayOfWeek? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val active: Boolean? = null
)

data class DoctorAvailabilityResponse(
    val id: UUID,
    val doctorId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)