package com.healthmarketplace.backend.modules.publicapi.patientportal.controller

import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.PatientPortalProfileResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.UpdatePatientPortalProfileRequest
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.PatientPortalAppointmentResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.service.PatientPortalProfileService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/public/patient-portal")
@PreAuthorize("hasRole('PATIENT')")
class PatientPortalController(
    private val patientPortalProfileService: PatientPortalProfileService
) {

    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal jwt: Jwt
    ): PatientPortalProfileResponse {
        return patientPortalProfileService.getMe(jwt)
    }

    @PatchMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: UpdatePatientPortalProfileRequest
    ): PatientPortalProfileResponse {
        return patientPortalProfileService.updateMe(jwt, request)
    }

    @PostMapping("/me/avatar")
    fun updateAvatar(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("file") file: MultipartFile
    ): PatientPortalProfileResponse {
        return patientPortalProfileService.updateAvatar(jwt, file)
    }

    @GetMapping("/appointments")
    fun getAppointments(@AuthenticationPrincipal jwt: Jwt): List<PatientPortalAppointmentResponse> {
        return patientPortalProfileService.appointments(jwt)
    }
}
