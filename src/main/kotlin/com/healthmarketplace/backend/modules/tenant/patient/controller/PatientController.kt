package com.healthmarketplace.backend.modules.tenant.patient.controller

import com.healthmarketplace.backend.modules.tenant.patient.dto.CreatePatientRequest
import com.healthmarketplace.backend.modules.tenant.patient.dto.PatientResponse
import com.healthmarketplace.backend.modules.tenant.patient.dto.UpdatePatientRequest
import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import com.healthmarketplace.backend.modules.tenant.patient.service.PatientService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/patients")
class PatientController(
    private val patientService: PatientService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findAll(
        @RequestParam(required = false) status: PatientStatus?,
        @RequestParam(required = false) search: String?
    ): List<PatientResponse> {
        return patientService.findAll(
            status = status,
            search = search
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findById(
        @PathVariable id: UUID
    ): PatientResponse {
        return patientService.findById(id)
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun profile(
        @PathVariable id: UUID
    ): PatientResponse {
        return patientService.findById(id)
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun create(
        @RequestBody request: CreatePatientRequest
    ): PatientResponse {
        return patientService.create(request)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdatePatientRequest
    ): PatientResponse {
        return patientService.update(id, request)
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun deactivate(
        @PathVariable id: UUID
    ): PatientResponse {
        return patientService.deactivate(id)
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun activate(
        @PathVariable id: UUID
    ): PatientResponse {
        return patientService.activate(id)
    }
}