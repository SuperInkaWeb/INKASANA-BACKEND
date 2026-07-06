package com.healthmarketplace.backend.modules.tenant.debug.controller

import com.healthmarketplace.backend.config.multitenancy.TenantContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tenant/debug")
class TenantDebugController {

    @GetMapping("/current-tenant")
    fun currentTenant(): Map<String, String> {
        return mapOf(
            "tenant" to TenantContext.getTenant()
        )
    }
}