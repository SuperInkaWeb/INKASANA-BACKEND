package com.healthmarketplace.backend.modules.core.specialty.controller

import com.healthmarketplace.backend.modules.core.specialty.dto.CreateGlobalSpecialtyRequest
import com.healthmarketplace.backend.modules.core.specialty.dto.GlobalSpecialtyResponse
import com.healthmarketplace.backend.modules.core.specialty.dto.UpdateGlobalSpecialtyRequest
import com.healthmarketplace.backend.modules.core.specialty.model.GlobalSpecialtyStatus
import com.healthmarketplace.backend.modules.core.specialty.service.GlobalSpecialtyService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/platform/specialties")
class GlobalSpecialtyController(
    private val service: GlobalSpecialtyService
) {

    @GetMapping
    fun findAll(
        @RequestParam(required = false) status: GlobalSpecialtyStatus?,
        @RequestParam(required = false) search: String?
    ): List<GlobalSpecialtyResponse> {
        return service.findAll(status, search)
    }

    @GetMapping("/active")
    fun findActive(): List<GlobalSpecialtyResponse> {
        return service.findActive()
    }

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID
    ): GlobalSpecialtyResponse {
        return service.findById(id)
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateGlobalSpecialtyRequest
    ): GlobalSpecialtyResponse {
        return service.create(request)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateGlobalSpecialtyRequest
    ): GlobalSpecialtyResponse {
        return service.update(id, request)
    }

    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: UUID
    ): GlobalSpecialtyResponse {
        return service.activate(id)
    }

    @PatchMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: UUID
    ): GlobalSpecialtyResponse {
        return service.deactivate(id)
    }
}