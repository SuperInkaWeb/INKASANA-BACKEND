package com.healthmarketplace.backend.modules.tenant.branding.controller

import com.healthmarketplace.backend.modules.tenant.branding.dto.TenantBrandingRequest
import com.healthmarketplace.backend.modules.tenant.branding.dto.TenantBrandingResponse
import com.healthmarketplace.backend.modules.tenant.branding.service.TenantBrandingService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tenant/branding")
class TenantBrandingController(
    private val tenantBrandingService: TenantBrandingService
) {

    @GetMapping
    fun getBranding(): TenantBrandingResponse {

        return tenantBrandingService.getBranding()
    }

    @PostMapping
    fun saveBranding(
        @RequestBody request: TenantBrandingRequest
    ): TenantBrandingResponse {

        return tenantBrandingService.saveBranding(request)
    }
}