package com.healthmarketplace.backend.modules.tenant.doctor.dto

import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorVerificationStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class DoctorResponse(
    val id: UUID,
    val tenantUserId: UUID?,
    val fullName: String,
    val specialty: String?,
    val licenseNumber: String?,
    val email: String?,
    val phone: String?,
    val status: String,
    val verificationStatus: DoctorVerificationStatus,
    val verifiedAt: LocalDateTime?,
    val verifiedBy: UUID?,
    val rejectionReason: String?,
    val bio: String?,
    val consultationPrice: BigDecimal?,
    val consultationDurationMinutes: Int?,
    val specialties: List<DoctorSpecialtyResponse> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)