package com.healthmarketplace.backend.modules.publicapi.auth.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/security-test")
class SecurityTestController {

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    fun adminOnly(): Map<String, String> {
        return mapOf(
            "message" to "Acceso permitido para ADMIN o SUPER_ADMIN"
        )
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'SUPER_ADMIN')")
    fun doctorOnly(): Map<String, String> {
        return mapOf(
            "message" to "Acceso permitido para DOCTOR, ADMIN o SUPER_ADMIN"
        )
    }

    @GetMapping("/patient")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN', 'SUPER_ADMIN')")
    fun patientOnly(): Map<String, String> {
        return mapOf(
            "message" to "Acceso permitido para PATIENT, ADMIN o SUPER_ADMIN"
        )
    }

    @GetMapping("/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun superAdminOnly(): Map<String, String> {
        return mapOf(
            "message" to "Acceso permitido para SUPER_ADMIN"
        )
    }
}