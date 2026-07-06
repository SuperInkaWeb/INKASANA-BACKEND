package com.healthmarketplace.backend.config.multitenancy

import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class TenantMigrationService(
    private val dataSource: DataSource,
    private val organizationRepository: OrganizationRepository,

    @Value("\${app.tenant.migration-location}")
    private val tenantMigrationLocation: String
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        migrateAllTenants()
    }

    fun migrateAllTenants() {
        val organizations = organizationRepository.findAll()

        organizations
            .filter { it.schemaReady }
            .forEach { organization ->
                migrateTenantSchema(organization.schemaName)
            }
    }

    fun migrateTenantSchema(schemaName: String) {
        println("Ejecutando migraciones tenant para schema -> $schemaName")

        Flyway.configure()
            .dataSource(dataSource)
            .schemas(schemaName)
            .defaultSchema(schemaName)
            .locations(tenantMigrationLocation)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .load()
            .migrate()

        println("Migraciones tenant completadas -> $schemaName")
    }
}