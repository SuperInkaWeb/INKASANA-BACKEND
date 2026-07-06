package com.healthmarketplace.backend.modules.core.organization.repository

import com.healthmarketplace.backend.modules.core.organization.entity.Organization
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationRepository : JpaRepository<Organization, UUID> {

    fun existsBySlug(slug: String): Boolean

    fun existsBySchemaName(schemaName: String): Boolean

    fun findBySlug(slug: String): Optional<Organization>

    fun findBySchemaName(schemaName: String): Optional<Organization>

    fun findByIdAndStatus(id: UUID, status: OrganizationStatus): Optional<Organization>

    fun findBySlugAndStatus(slug: String, status: OrganizationStatus): Optional<Organization>

    fun findBySchemaNameAndStatus(
        schemaName: String,
        status: OrganizationStatus
    ): Optional<Organization>
}