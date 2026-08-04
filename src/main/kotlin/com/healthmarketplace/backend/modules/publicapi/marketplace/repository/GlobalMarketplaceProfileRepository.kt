package com.healthmarketplace.backend.modules.publicapi.marketplace.repository

import com.healthmarketplace.backend.modules.publicapi.marketplace.entity.GlobalMarketplaceProfile
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface GlobalMarketplaceProfileRepository : JpaRepository<GlobalMarketplaceProfile, UUID> {

    fun findBySlug(slug: String): GlobalMarketplaceProfile?

    fun findBySourceProfileId(sourceProfileId: UUID): Optional<GlobalMarketplaceProfile>

    fun findBySourceDoctorId(sourceDoctorId: UUID): GlobalMarketplaceProfile?
    fun findAllByProfileTypeAndIsPublishedTrueAndStatus(
        profileType: MarketplaceProfileType,
        status: MarketplaceProfileStatus
    ): List<GlobalMarketplaceProfile>

    fun findAllByProfileTypeAndIsPublishedTrueAndStatusAndTenantSlug(
        profileType: MarketplaceProfileType,
        status: MarketplaceProfileStatus,
        tenantSlug: String
    ): List<GlobalMarketplaceProfile>

    fun deleteBySourceProfileId(sourceProfileId: UUID)
}