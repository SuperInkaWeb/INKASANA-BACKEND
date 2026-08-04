package com.healthmarketplace.backend.modules.tenant.agenda.controller

import com.healthmarketplace.backend.modules.tenant.agenda.dto.AvailabilityExceptionResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.CreateAvailabilityExceptionRequest
import com.healthmarketplace.backend.modules.tenant.agenda.dto.UpdateAvailabilityExceptionRequest
import com.healthmarketplace.backend.modules.tenant.agenda.service.AvailabilityExceptionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/doctors/{doctorId}/availability-exceptions")
class AvailabilityExceptionController(
    private val availabilityExceptionService: AvailabilityExceptionService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findAll(
        @PathVariable doctorId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<AvailabilityExceptionResponse> {
        return availabilityExceptionService.findAllByDoctor(doctorId, from, to)
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun create(
        @PathVariable doctorId: UUID,
        @RequestBody request: CreateAvailabilityExceptionRequest
    ): AvailabilityExceptionResponse {
        return availabilityExceptionService.create(doctorId, request)
    }

    @PatchMapping("/{exceptionId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun update(
        @PathVariable doctorId: UUID,
        @PathVariable exceptionId: UUID,
        @RequestBody request: UpdateAvailabilityExceptionRequest
    ): AvailabilityExceptionResponse {
        return availabilityExceptionService.update(doctorId, exceptionId, request)
    }

    @DeleteMapping("/{exceptionId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR')")
    fun delete(
        @PathVariable doctorId: UUID,
        @PathVariable exceptionId: UUID
    ) {
        availabilityExceptionService.delete(doctorId, exceptionId)
    }
}