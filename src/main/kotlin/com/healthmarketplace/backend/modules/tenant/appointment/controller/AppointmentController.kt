package com.healthmarketplace.backend.modules.tenant.appointment.controller

import com.healthmarketplace.backend.modules.tenant.appointment.dto.*
import com.healthmarketplace.backend.modules.tenant.appointment.service.AppointmentService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/appointments")
class AppointmentController(private val appointmentService: AppointmentService) {
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun summary() = appointmentService.summary()

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST','PATIENT')")
    fun create(@Valid @RequestBody request: CreateAppointmentRequest) = appointmentService.create(request)

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST','PATIENT')")
    fun findAll(
        @RequestParam(required = false) patientId: UUID?,
        @RequestParam(required = false) doctorId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @AuthenticationPrincipal jwt: Jwt
    ) = appointmentService.findAllForUser(
        patientId = patientId,
        doctorId = doctorId,
        date = date,
        currentUserId = UUID.fromString(jwt.subject),
        currentRole = jwt.getClaimAsString("role")
    )

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun changeStatus(@PathVariable id: UUID, @Valid @RequestBody request: UpdateAppointmentStatusRequest) = appointmentService.changeStatus(id, request)

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST','PATIENT')")
    fun cancel(@PathVariable id: UUID) = appointmentService.cancel(id)
}
