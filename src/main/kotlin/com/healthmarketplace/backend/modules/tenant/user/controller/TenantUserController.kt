package com.healthmarketplace.backend.modules.tenant.user.controller

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.user.dto.CreateTenantUserRequest
import com.healthmarketplace.backend.modules.tenant.user.dto.TenantUserResponse
import com.healthmarketplace.backend.modules.tenant.user.dto.UpdateTenantUserRequest
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.service.TenantUserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/users")
class TenantUserController(
    private val tenantUserService: TenantUserService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun findAll(
        @RequestParam(required = false) status: TenantUserStatus?,
        @RequestParam(required = false) role: TenantUserRole?,
        @RequestParam(required = false) search: String?
    ): List<TenantUserResponse> {
        return tenantUserService.findAll(status, role, search)
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST','PATIENT')")
    fun me(
        authentication: JwtAuthenticationToken
    ): TenantUserResponse {
        val email = authentication.token.getClaimAsString("email")
            ?: throw BusinessException("El token no contiene email")

        return tenantUserService.findMe(email)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun findById(
        @PathVariable id: UUID
    ): TenantUserResponse {
        return tenantUserService.findById(id)
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun create(
        @RequestBody request: CreateTenantUserRequest
    ): TenantUserResponse {
        return tenantUserService.create(request)
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTenantUserRequest
    ): TenantUserResponse {
        return tenantUserService.update(id, request)
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun activate(
        @PathVariable id: UUID
    ): TenantUserResponse {
        return tenantUserService.activate(id)
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun suspend(
        @PathVariable id: UUID
    ): TenantUserResponse {
        return tenantUserService.suspend(id)
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun deactivate(
        @PathVariable id: UUID
    ): TenantUserResponse {
        return tenantUserService.deactivate(id)
    }
}