package com.healthmarketplace.backend.modules.tenant.branding.repository

import com.healthmarketplace.backend.modules.tenant.branding.entity.TenantBranding
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantBrandingRepository :
    JpaRepository<TenantBranding, UUID> {

    fun findTopByOrderByCreatedAtAsc(): TenantBranding?
}