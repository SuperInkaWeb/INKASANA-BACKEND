package com.healthmarketplace.backend.modules.publicapi.auth.controller

import com.healthmarketplace.backend.modules.publicapi.auth.dto.Auth0TenantLoginRequest
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginTokenResponse
import com.healthmarketplace.backend.modules.publicapi.auth.service.TenantAuthService
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class Auth0TenantAuthController(
    private val tenantAuthService: TenantAuthService
) {

    @PostMapping("/auth0-tenant-login")
    fun auth0TenantLogin(
        @RequestBody request: Auth0TenantLoginRequest,
        authentication: JwtAuthenticationToken
    ): TenantLoginTokenResponse {

        return tenantAuthService.loginWithAuth0(
            slug = request.slug,
            email = request.email,
            auth0Id = request.auth0Id
        )
    }
}