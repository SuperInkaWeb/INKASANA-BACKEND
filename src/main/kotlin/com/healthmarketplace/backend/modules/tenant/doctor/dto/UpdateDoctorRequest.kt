package com.healthmarketplace.backend.modules.tenant.doctor.dto

import java.math.BigDecimal
import java.util.UUID

data class UpdateDoctorRequest(
    val tenantUserId: UUID? = null,
    val fullName: String? = null,
    val specialty: String? = null,
    val licenseNumber: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val consultationPrice: BigDecimal? = null,
    val consultationDurationMinutes: Int? = null,
    val specialtyIds: List<UUID> = emptyList()

)