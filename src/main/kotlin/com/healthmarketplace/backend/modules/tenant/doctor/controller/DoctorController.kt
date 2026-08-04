package com.healthmarketplace.backend.modules.tenant.doctor.controller

import com.healthmarketplace.backend.modules.tenant.doctor.dto.CreateDoctorRequest
import com.healthmarketplace.backend.modules.tenant.doctor.dto.DoctorResponse
import com.healthmarketplace.backend.modules.tenant.doctor.dto.RejectDoctorRequest
import com.healthmarketplace.backend.modules.tenant.doctor.dto.UpdateDoctorRequest
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import com.healthmarketplace.backend.modules.tenant.doctor.service.DoctorService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/doctors")
class DoctorController(
    private val doctorService: DoctorService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findAll(
        @RequestParam(required = false) status: DoctorStatus?,
        @RequestParam(required = false) search: String?
    ): List<DoctorResponse> {
        return doctorService.findAll(
            status = status,
            search = search
        )
    }

    @GetMapping("/{id}") // solo sirve para buscar o listar
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findById(
        @PathVariable id: UUID
    ): DoctorResponse {
        return doctorService.findById(id)
    }

    @PostMapping //creo los doctores
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun create(
        @RequestBody request: CreateDoctorRequest
    ): DoctorResponse {
        return doctorService.create(request)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateDoctorRequest
    ): DoctorResponse {
        return doctorService.update(id, request)
    }

    @PostMapping("/{id}/photo") // sube/reemplaza la foto de perfil del doctor (drag & drop)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun uploadPhoto(
        @PathVariable id: UUID,
        @RequestParam("photo") photo: MultipartFile
    ): DoctorResponse {
        return doctorService.updateProfilePhoto(id, photo)
    }

    @PatchMapping("/{id}/deactivate") //patch edita los edoctores , sirve para editar
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun deactivate(
        @PathVariable id: UUID
    ): DoctorResponse {
        return doctorService.deactivate(id)
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun activate(
        @PathVariable id: UUID
    ): DoctorResponse {
        return doctorService.activate(id)
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun approveDoctor(
        @PathVariable id: UUID,
        authentication: Authentication
    ): DoctorResponse {
        val userId = UUID.fromString(authentication.name)

        return doctorService.approveDoctor(
            id = id,
            verifiedBy = userId
        )
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun rejectDoctor(
        @PathVariable id: UUID,
        @RequestBody request: RejectDoctorRequest,
        authentication: Authentication
    ): DoctorResponse {
        val userId = UUID.fromString(authentication.name)

        return doctorService.rejectDoctor(
            id = id,
            verifiedBy = userId,
            reason = request.reason
        )
    }
}