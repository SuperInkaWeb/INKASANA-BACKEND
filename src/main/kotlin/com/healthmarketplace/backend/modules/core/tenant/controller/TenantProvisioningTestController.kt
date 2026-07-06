package com.healthmarketplace.backend.modules.core.tenant.controller

import com.healthmarketplace.backend.modules.core.tenant.service.TenantProvisioningService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/test/provision")
class TenantProvisioningTestController(
    private val tenantProvisioningService: TenantProvisioningService
) {

    @PostMapping("/{schema}")
    fun provision(
        @PathVariable schema: String
    ): String {

        tenantProvisioningService.provision(schema)

        return "Tenant provisionado correctamente: $schema"
    }
}