package com.healthmarketplace.backend.modules.publicapi.marketplace.dto

import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CreatePublicAppointmentCheckoutRequest(
    @field:NotNull val doctorId: UUID,
    @field:NotNull @field:FutureOrPresent val date: LocalDate,
    @field:NotNull val time: LocalTime
)

data class PublicAppointmentCheckoutResponse(
    val url: String
)
