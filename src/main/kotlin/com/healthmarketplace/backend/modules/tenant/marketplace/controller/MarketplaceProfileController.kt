package com.healthmarketplace.backend.modules.tenant.marketplace.controller

import com.healthmarketplace.backend.modules.tenant.marketplace.dto.CreateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.MarketplaceProfileResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.UpdateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.service.MarketplaceProfileService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tenant/marketplace/profiles")
class MarketplaceProfileController(
    private val marketplaceProfileService: MarketplaceProfileService
) {

    @GetMapping
    fun findAll(): List<MarketplaceProfileResponse> {
        return marketplaceProfileService.findAll()
    }

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID
    ): MarketplaceProfileResponse {
        return marketplaceProfileService.findById(id)
    }

    @PostMapping
    fun create(
        @RequestBody request: CreateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {
        return marketplaceProfileService.create(request)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {
        return marketplaceProfileService.update(id, request)
    }

    @PatchMapping("/{id}/publish")
    fun publish(
        @PathVariable id: UUID
    ): MarketplaceProfileResponse {
        return marketplaceProfileService.publish(id)
    }

    @PatchMapping("/{id}/hide")
    fun hide(
        @PathVariable id: UUID
    ): MarketplaceProfileResponse {
        return marketplaceProfileService.hide(id)
    }
}