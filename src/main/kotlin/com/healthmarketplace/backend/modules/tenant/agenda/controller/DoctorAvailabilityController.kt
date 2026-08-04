package com.healthmarketplace.backend.modules.tenant.agenda.controller

import com.healthmarketplace.backend.modules.tenant.agenda.dto.CreateDoctorAvailabilityRequest
import com.healthmarketplace.backend.modules.tenant.agenda.dto.DoctorAvailabilityResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.UpdateDoctorAvailabilityRequest
import com.healthmarketplace.backend.modules.tenant.agenda.service.DoctorAvailabilityService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/doctors/{doctorId}/availability")
class DoctorAvailabilityController(
    private val doctorAvailabilityService: DoctorAvailabilityService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findAll(
        @PathVariable doctorId: UUID
    ): List<DoctorAvailabilityResponse> {
        return doctorAvailabilityService.findAllByDoctor(doctorId)
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun create(
        @PathVariable doctorId: UUID,
        @RequestBody request: CreateDoctorAvailabilityRequest
    ): DoctorAvailabilityResponse {
        return doctorAvailabilityService.create(doctorId, request)
    }

    @PatchMapping("/{availabilityId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun update(
        @PathVariable doctorId: UUID,
        @PathVariable availabilityId: UUID,
        @RequestBody request: UpdateDoctorAvailabilityRequest
    ): DoctorAvailabilityResponse {
        return doctorAvailabilityService.update(doctorId, availabilityId, request)
    }

    @DeleteMapping("/{availabilityId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun delete(
        @PathVariable doctorId: UUID,
        @PathVariable availabilityId: UUID
    ) {
        doctorAvailabilityService.delete(doctorId, availabilityId)
    }
}