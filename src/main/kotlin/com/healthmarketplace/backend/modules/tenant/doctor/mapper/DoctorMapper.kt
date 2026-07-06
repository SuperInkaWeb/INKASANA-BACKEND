package com.healthmarketplace.backend.modules.tenant.doctor.mapper

import com.healthmarketplace.backend.modules.tenant.doctor.dto.DoctorResponse
import com.healthmarketplace.backend.modules.tenant.doctor.dto.DoctorSpecialtyResponse
import com.healthmarketplace.backend.modules.tenant.doctor.entity.Doctor

fun Doctor.toResponse(
    specialties: List<DoctorSpecialtyResponse> = emptyList()
): DoctorResponse {
    return DoctorResponse(
        id = this.id!!,
        tenantUserId = this.tenantUserId,
        fullName = this.fullName,
        specialty = this.specialty,
        licenseNumber = this.licenseNumber,
        email = this.email,
        phone = this.phone,
        status = this.status.name,
        verificationStatus = this.verificationStatus,
        verifiedAt = this.verifiedAt,
        verifiedBy = this.verifiedBy,
        rejectionReason = this.rejectionReason,
        bio = this.bio,
        consultationPrice = this.consultationPrice,
        consultationDurationMinutes = this.consultationDurationMinutes,
        specialties = specialties,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}