package com.healthmarketplace.backend.modules.tenant.doctor.dto

import jakarta.validation.constraints.NotBlank

data class RejectDoctorRequest(

    @field:NotBlank(message = "El motivo de rechazo es obligatorio")
    val reason: String
)