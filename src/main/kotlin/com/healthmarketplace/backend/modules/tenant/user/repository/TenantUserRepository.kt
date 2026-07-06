package com.healthmarketplace.backend.modules.tenant.user.repository

import com.healthmarketplace.backend.modules.tenant.user.entity.TenantUser
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface TenantUserRepository : JpaRepository<TenantUser, UUID> {

    fun findByEmail(email: String): Optional<TenantUser>

    fun existsByEmail(email: String): Boolean

    fun findByEmailAndStatus(
        email: String,
        status: TenantUserStatus
    ): Optional<TenantUser>

    fun findAllByStatus(
        status: TenantUserStatus
    ): List<TenantUser>

    fun findAllByRole(
        role: TenantUserRole
    ): List<TenantUser>

    fun findAllByRoleAndStatus(
        role: TenantUserRole,
        status: TenantUserStatus
    ): List<TenantUser>

    fun findAllByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        fullName: String,
        email: String
    ): List<TenantUser>

    fun findAllByOrderByCreatedAtDesc(): List<TenantUser>
}