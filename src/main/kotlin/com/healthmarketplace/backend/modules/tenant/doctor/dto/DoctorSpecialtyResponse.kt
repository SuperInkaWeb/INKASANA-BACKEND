package com.healthmarketplace.backend.modules.tenant.doctor.dto

import java.util.UUID

data class DoctorSpecialtyResponse(
    val id: UUID,
    val name: String,
    val description: String?
)