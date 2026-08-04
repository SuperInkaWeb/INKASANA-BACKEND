package com.healthmarketplace.backend.modules.tenant.marketplace.service

import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.healthmarketplace.backend.modules.publicapi.marketplace.entity.GlobalMarketplaceProfile
import com.healthmarketplace.backend.modules.publicapi.marketplace.repository.GlobalMarketplaceProfileRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.entity.MarketplaceProfile
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MarketplaceSyncService(
    private val globalMarketplaceProfileRepository: GlobalMarketplaceProfileRepository,
    private val organizationRepository: OrganizationRepository
) {

    fun syncPublishedProfile(profile: MarketplaceProfile) {
        val sourceProfileId = profile.id ?: return
        val currentSchema = TenantContext.getTenant()

        val organization = organizationRepository
            .findBySchemaNameAndStatus(currentSchema, OrganizationStatus.ACTIVE)
            .orElse(null)
            ?: return

        val existingGlobalProfile = globalMarketplaceProfileRepository
            .findBySourceProfileId(sourceProfileId)
            .orElse(null)

        val globalProfile =
            existingGlobalProfile ?: GlobalMarketplaceProfile(
                sourceProfileId = sourceProfileId,
                sourceDoctorId = profile.doctorId,
                sourceOrganizationId = profile.organizationId,
                tenantSlug = organization.slug,
                schemaName = organization.schemaName,
                profileType = profile.profileType,
                displayName = profile.displayName,
                slug = profile.slug,
                headline = profile.headline,
                description = profile.description,
                city = profile.city,
                country = profile.country,
                address = profile.address,
                phone = profile.phone,
                email = profile.email,
                profileImageUrl = profile.profileImageUrl,
                coverImageUrl = profile.coverImageUrl,
                carouselImageUrl1 = profile.carouselImageUrl1,
                carouselImageUrl2 = profile.carouselImageUrl2,
                pageColor = profile.pageColor,
                buttonColor = profile.buttonColor,
                subscriptionColor = profile.subscriptionColor,
                appearanceConfig = profile.appearanceConfig,
                consultationPrice = profile.consultationPrice,
                consultationDurationMinutes = profile.consultationDurationMinutes,
                availableDays = profile.availableDays,
                availableStartTime = profile.availableStartTime,
                availableEndTime = profile.availableEndTime,
                specialties = profile.specialties,
                isPublished = true,
                status = MarketplaceProfileStatus.PUBLISHED,
                publishedAt = LocalDateTime.now()
            )

        globalProfile.sourceDoctorId = profile.doctorId
        globalProfile.sourceOrganizationId = profile.organizationId
        globalProfile.tenantSlug = organization.slug
        globalProfile.schemaName = organization.schemaName
        globalProfile.profileType = profile.profileType
        globalProfile.displayName = profile.displayName
        globalProfile.slug = profile.slug
        globalProfile.headline = profile.headline
        globalProfile.description = profile.description
        globalProfile.city = profile.city
        globalProfile.country = profile.country
        globalProfile.address = profile.address
        globalProfile.phone = profile.phone
        globalProfile.email = profile.email
        globalProfile.profileImageUrl = profile.profileImageUrl
        globalProfile.coverImageUrl = profile.coverImageUrl
        globalProfile.carouselImageUrl1 = profile.carouselImageUrl1
        globalProfile.carouselImageUrl2 = profile.carouselImageUrl2
        globalProfile.pageColor = profile.pageColor
        globalProfile.buttonColor = profile.buttonColor
        globalProfile.subscriptionColor = profile.subscriptionColor
        globalProfile.appearanceConfig = profile.appearanceConfig
        globalProfile.consultationPrice = profile.consultationPrice
        globalProfile.consultationDurationMinutes = profile.consultationDurationMinutes
        globalProfile.availableDays = profile.availableDays
        globalProfile.availableStartTime = profile.availableStartTime
        globalProfile.availableEndTime = profile.availableEndTime
        globalProfile.specialties = profile.specialties
        globalProfile.isPublished = true
        globalProfile.status = MarketplaceProfileStatus.PUBLISHED
        globalProfile.publishedAt = profile.publishedAt ?: LocalDateTime.now()
        globalProfile.updatedAt = LocalDateTime.now()

        globalMarketplaceProfileRepository.save(globalProfile)
    }

    fun removePublishedProfile(profile: MarketplaceProfile) {
        val sourceProfileId = profile.id ?: return

        val globalProfile = globalMarketplaceProfileRepository
            .findBySourceProfileId(sourceProfileId)
            .orElse(null)
            ?: return

        globalProfile.isPublished = false
        globalProfile.status = MarketplaceProfileStatus.DRAFT
        globalProfile.updatedAt = LocalDateTime.now()

        globalMarketplaceProfileRepository.save(globalProfile)
    }
}
