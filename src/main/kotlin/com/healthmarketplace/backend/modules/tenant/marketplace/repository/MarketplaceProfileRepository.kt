package com.healthmarketplace.backend.modules.tenant.marketplace.repository

import com.healthmarketplace.backend.modules.tenant.marketplace.entity.MarketplaceProfile
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MarketplaceProfileRepository : JpaRepository<MarketplaceProfile, UUID> {

    fun existsBySlug(slug: String): Boolean

    fun findBySlug(slug: String): MarketplaceProfile?

    fun findByDoctorId(doctorId: UUID): MarketplaceProfile?

    fun findAllByStatus(status: MarketplaceProfileStatus): List<MarketplaceProfile>

    fun findAllByIsPublishedTrue(): List<MarketplaceProfile>

    fun findAllByProfileTypeAndIsPublishedTrueAndStatus(
        profileType: MarketplaceProfileType,
        status: MarketplaceProfileStatus
    ): List<MarketplaceProfile>


}