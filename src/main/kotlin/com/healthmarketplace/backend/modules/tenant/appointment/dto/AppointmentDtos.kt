package com.healthmarketplace.backend.modules.tenant.appointment.dto

import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class CreateAppointmentRequest(
    @field:NotNull val patientId: UUID,
    @field:NotNull val doctorId: UUID,
    @field:NotNull @field:FutureOrPresent val date: LocalDate,
    @field:NotNull val time: LocalTime,
    val reason: String? = null
)

data class UpdateAppointmentStatusRequest(@field:NotNull val status: AppointmentStatus)

data class AppointmentResponse(
    val id: UUID,
    val patientId: UUID,
    val patientName: String,
    val doctorId: UUID,
    val doctorName: String,
    val tenantId: String,
    val date: LocalDate,
    val time: LocalTime,
    val status: AppointmentStatus,
    val reason: String?,
    val price: BigDecimal?,
    val createdAt: LocalDateTime
)

/** Métricas ligeras para la portada del dashboard del tenant. */
data class AppointmentSummaryResponse(
    val confirmedAppointments: Long
)
