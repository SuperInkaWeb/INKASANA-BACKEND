package com.healthmarketplace.backend.modules.tenant.bootstrap.service

import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.tenant.user.entity.TenantUser
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import org.springframework.stereotype.Service

@Service
class TenantBootstrapService(
    private val tenantUserRepository: TenantUserRepository
) {

    fun createOwner(
        schemaName: String,
        ownerEmail: String,
        ownerFullName: String
    ): TenantUser {
        return try {
            TenantContext.setTenant(schemaName)

            val cleanEmail = ownerEmail.trim().lowercase()
            val cleanName = ownerFullName.trim()

            val exists = tenantUserRepository.findByEmail(cleanEmail).isPresent

            if (exists) {
                throw IllegalArgumentException("Ya existe un OWNER con el email $cleanEmail")
            }

            tenantUserRepository.save(
                TenantUser(
                    email = cleanEmail,
                    fullName = cleanName,
                    role = TenantUserRole.OWNER,
                    status = TenantUserStatus.ACTIVE
                )
            )
        } finally {
            TenantContext.clear()
        }
    }
}