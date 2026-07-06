package com.healthmarketplace.backend.modules.core.organization.controller

import com.healthmarketplace.backend.modules.core.organization.dto.CreateOrganizationRequest
import com.healthmarketplace.backend.modules.core.organization.dto.OrganizationResponse
import com.healthmarketplace.backend.modules.core.organization.dto.UpdateOrganizationStatusRequest
import com.healthmarketplace.backend.modules.core.organization.service.OrganizationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/platform/organizations")
class OrganizationController(
    private val organizationService: OrganizationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateOrganizationRequest
    ): OrganizationResponse {
        return organizationService.create(request)
    }

    @GetMapping
    fun findAll(): List<OrganizationResponse> {
        return organizationService.findAll()
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody request: UpdateOrganizationStatusRequest
    ): OrganizationResponse {
        return organizationService.updateStatus(id, request)
    }
}