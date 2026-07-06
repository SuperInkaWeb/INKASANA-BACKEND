package com.healthmarketplace.backend.config.multitenancy

import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class HibernateMultiTenantConfig(
    private val tenantIdentifierResolver: TenantIdentifierResolver,
    private val multiTenantConnectionProvider: SchemaMultiTenantConnectionProvider
) : HibernatePropertiesCustomizer {

    override fun customize(hibernateProperties: MutableMap<String, Any>) {
        hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] =
            tenantIdentifierResolver

        hibernateProperties[AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER] =
            multiTenantConnectionProvider
    }
}