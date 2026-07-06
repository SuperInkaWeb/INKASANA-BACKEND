package com.healthmarketplace.backend.modules.core.tenant.service

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class TenantProvisioningService(
    private val dataSource: DataSource
) {

    @Value("\${app.tenant.migration-location}")
    private lateinit var tenantMigrationLocation: String

    fun provision(schemaName: String) {
        val safeSchema = sanitizeSchemaName(schemaName)

        createSchema(safeSchema)
        migrateSchema(safeSchema)
    }

    private fun createSchema(schemaName: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """CREATE SCHEMA IF NOT EXISTS "$schemaName""""
                )
            }
        }
    }

    private fun migrateSchema(schemaName: String) {
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(schemaName)
            .defaultSchema(schemaName)
            .locations(tenantMigrationLocation)
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    private fun sanitizeSchemaName(schemaName: String): String {
        val clean = schemaName.trim()

        require(clean.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            "Nombre de schema inválido: $schemaName"
        }

        return clean
    }
}