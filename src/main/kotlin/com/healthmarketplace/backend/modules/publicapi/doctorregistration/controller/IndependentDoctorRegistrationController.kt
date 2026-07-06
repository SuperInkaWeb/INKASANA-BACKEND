package com.healthmarketplace.backend.modules.publicapi.doctorregistration.controller

import com.healthmarketplace.backend.modules.publicapi.doctorregistration.dto.RegisterIndependentDoctorRequest
import com.healthmarketplace.backend.modules.publicapi.doctorregistration.dto.RegisterIndependentDoctorResponse
import com.healthmarketplace.backend.modules.publicapi.doctorregistration.service.IndependentDoctorRegistrationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/doctor-registration")
class IndependentDoctorRegistrationController(
    private val service: IndependentDoctorRegistrationService
) {

    @PostMapping
    fun register(
        @RequestBody request: RegisterIndependentDoctorRequest
    ): RegisterIndependentDoctorResponse {
        return service.register(request)
    }
}