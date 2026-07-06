package com.healthmarketplace.backend.modules.tenant.branding.service

import com.healthmarketplace.backend.modules.tenant.branding.dto.TenantBrandingRequest
import com.healthmarketplace.backend.modules.tenant.branding.dto.TenantBrandingResponse
import com.healthmarketplace.backend.modules.tenant.branding.entity.TenantBranding
import com.healthmarketplace.backend.modules.tenant.branding.repository.TenantBrandingRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TenantBrandingService(
    private val tenantBrandingRepository: TenantBrandingRepository
) {

    fun getBranding(): TenantBrandingResponse {

        val branding = tenantBrandingRepository
            .findTopByOrderByCreatedAtAsc()
            ?: createDefaultBranding()

        return mapToResponse(branding)
    }

    fun saveBranding(
        request: TenantBrandingRequest
    ): TenantBrandingResponse {

        val existing = tenantBrandingRepository
            .findTopByOrderByCreatedAtAsc()

        val branding = if (existing != null) {

            existing.clinicName = request.clinicName
            existing.slogan = request.slogan
            existing.primaryColor =
                request.primaryColor ?: "#1677ff"

            existing.secondaryColor =
                request.secondaryColor ?: "#001529"

            existing.logoUrl = request.logoUrl
            existing.faviconUrl = request.faviconUrl
            existing.contactEmail = request.contactEmail
            existing.contactPhone = request.contactPhone
            existing.address = request.address
            existing.city = request.city
            existing.country = request.country

            existing.onboardingCompleted =
                request.onboardingCompleted ?: false

            existing.updatedAt = LocalDateTime.now()

            existing

        } else {

            TenantBranding(
                clinicName = request.clinicName,
                slogan = request.slogan,
                primaryColor =
                    request.primaryColor ?: "#1677ff",

                secondaryColor =
                    request.secondaryColor ?: "#001529",

                logoUrl = request.logoUrl,
                faviconUrl = request.faviconUrl,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                address = request.address,
                city = request.city,
                country = request.country,

                onboardingCompleted =
                    request.onboardingCompleted ?: false
            )
        }

        val saved = tenantBrandingRepository.save(branding)

        return mapToResponse(saved)
    }

    private fun createDefaultBranding(): TenantBranding {

        val branding = TenantBranding(
            clinicName = "Medical Marketplace"
        )

        return tenantBrandingRepository.save(branding)
    }

    private fun mapToResponse(
        branding: TenantBranding
    ): TenantBrandingResponse {

        return TenantBrandingResponse(
            id = branding.id,
            clinicName = branding.clinicName,
            slogan = branding.slogan,
            primaryColor = branding.primaryColor,
            secondaryColor = branding.secondaryColor,
            logoUrl = branding.logoUrl,
            faviconUrl = branding.faviconUrl,
            contactEmail = branding.contactEmail,
            contactPhone = branding.contactPhone,
            address = branding.address,
            city = branding.city,
            country = branding.country,
            onboardingCompleted =
                branding.onboardingCompleted,
            createdAt = branding.createdAt,
            updatedAt = branding.updatedAt
        )
    }
}