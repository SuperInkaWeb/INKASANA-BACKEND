package com.healthmarketplace.backend.modules.publicapi.auth.controller

import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginRequest
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginResponse
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginTokenResponse
import com.healthmarketplace.backend.modules.publicapi.auth.service.TenantAuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/auth")
class TenantAuthController(
    private val tenantAuthService: TenantAuthService
) {

    @PostMapping("/tenant-login")
    fun tenantLogin(
        @RequestBody request: TenantLoginRequest
    ): TenantLoginResponse {

        return tenantAuthService.login(request)
    }

    @PostMapping("/tenant-login-token")
    fun tenantLoginToken(
        @RequestBody request: TenantLoginRequest
    ): TenantLoginTokenResponse {

        return tenantAuthService.loginWithToken(request)
    }
}