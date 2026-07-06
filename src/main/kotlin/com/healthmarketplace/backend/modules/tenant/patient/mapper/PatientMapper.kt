package com.healthmarketplace.backend.modules.tenant.patient.mapper

import com.healthmarketplace.backend.modules.tenant.patient.dto.PatientResponse
import com.healthmarketplace.backend.modules.tenant.patient.entity.Patient

fun Patient.toResponse(): PatientResponse {
    return PatientResponse(
        id = this.id!!,
        fullName = this.fullName,
        identification = this.identification,
        birthDate = this.birthDate,
        gender = this.gender,
        phone = this.phone,
        email = this.email,
        address = this.address,
        status = this.status,
        emergencyContactName = this.emergencyContactName,
        emergencyContactPhone = this.emergencyContactPhone,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}