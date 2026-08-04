package com.healthmarketplace.backend.modules.tenant.agenda.mapper
import com.healthmarketplace.backend.modules.tenant.agenda.dto.AvailabilityExceptionResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.DoctorAvailabilityResponse
import com.healthmarketplace.backend.modules.tenant.agenda.entity.AvailabilityException
import com.healthmarketplace.backend.modules.tenant.agenda.entity.DoctorAvailability

fun DoctorAvailability.toResponse(): DoctorAvailabilityResponse {
    return DoctorAvailabilityResponse(
        id = this.id!!,
        doctorId = this.doctorId,
        dayOfWeek = this.dayOfWeek,
        startTime = this.startTime,
        endTime = this.endTime,
        active = this.active,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun AvailabilityException.toResponse(): AvailabilityExceptionResponse {
    return AvailabilityExceptionResponse(
        id = this.id!!,
        doctorId = this.doctorId,
        exceptionDate = this.exceptionDate,
        type = this.type,
        startTime = this.startTime,
        endTime = this.endTime,
        reason = this.reason,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
