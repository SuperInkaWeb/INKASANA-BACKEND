package com.healthmarketplace.backend.config.multitenancy

import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.stereotype.Component

@Component
class TenantIdentifierResolver : CurrentTenantIdentifierResolver<String> {

    override fun resolveCurrentTenantIdentifier(): String {
        return TenantContext.getTenant()
    }

    override fun validateExistingCurrentSessions(): Boolean {
        return true
    }
}