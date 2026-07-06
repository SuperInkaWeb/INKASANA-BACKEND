package com.healthmarketplace.backend.modules.core.specialty.repository

import com.healthmarketplace.backend.modules.core.specialty.entity.GlobalSpecialty
import com.healthmarketplace.backend.modules.core.specialty.model.GlobalSpecialtyStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface GlobalSpecialtyRepository : JpaRepository<GlobalSpecialty, UUID> {

    fun findBySlug(slug: String): Optional<GlobalSpecialty>

    fun existsBySlug(slug: String): Boolean

    fun findAllByStatusOrderByNameAsc(
        status: GlobalSpecialtyStatus
    ): List<GlobalSpecialty>

    fun findAllByNameContainingIgnoreCaseOrderByNameAsc(
        search: String
    ): List<GlobalSpecialty>

    fun findAllByOrderByNameAsc(): List<GlobalSpecialty>
}