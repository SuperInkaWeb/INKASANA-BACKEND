package com.healthmarketplace.backend.modules.publicapi.marketplace.controller

import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceClinicDetailResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceClinicSearchResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceDoctorDetailResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceDoctorSearchResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.service.PublicMarketplaceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/public/marketplace")
class PublicMarketplaceController(
    private val publicMarketplaceService: PublicMarketplaceService
) {

    @GetMapping("/doctors")
    fun searchDoctors(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?
    ): List<MarketplaceDoctorSearchResponse> {
        return publicMarketplaceService.searchDoctors(
            search = search,
            city = city,
            country = country,
            minPrice = minPrice,
            maxPrice = maxPrice
        )
    }

    @GetMapping("/doctors/{slug}")
    fun getDoctorBySlug(
        @PathVariable slug: String
    ): MarketplaceDoctorDetailResponse {
        return publicMarketplaceService.getDoctorBySlug(slug)
    }

    @GetMapping("/clinics")
    fun searchClinics(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) country: String?
    ): List<MarketplaceClinicSearchResponse> {
        return publicMarketplaceService.searchClinics(
            search = search,
            city = city,
            country = country
        )
    }

    @GetMapping("/clinics/{slug}")
    fun getClinicBySlug(
        @PathVariable slug: String
    ): MarketplaceClinicDetailResponse {
        return publicMarketplaceService.getClinicBySlug(slug)
    }
}