package com.healthmarketplace.backend.config.multitenancy

object TenantContext {

    private val currentTenant = ThreadLocal<String>()

    const val DEFAULT_TENANT = "public"

    fun setTenant(schemaName: String?) {
        currentTenant.set(schemaName?.takeIf { it.isNotBlank() } ?: DEFAULT_TENANT)
    }

    fun getTenant(): String {
        return currentTenant.get() ?: DEFAULT_TENANT
    }

    fun clear() {
        currentTenant.remove()
    }
}