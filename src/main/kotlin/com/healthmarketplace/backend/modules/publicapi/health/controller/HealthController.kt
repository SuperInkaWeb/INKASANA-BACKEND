package com.healthmarketplace.backend.modules.publicapi.health.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "medical-marketplace-backend"
        )
    }
}