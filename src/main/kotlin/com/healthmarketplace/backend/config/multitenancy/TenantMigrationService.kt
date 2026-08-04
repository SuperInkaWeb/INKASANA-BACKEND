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
            .filter { it.schemaReady }

        val failedSchemas = mutableListOf<String>()

        // IMPORTANTE: cada tenant se migra de forma aislada. Antes, si UNA
        // sola clínica fallaba al migrar, la excepción cortaba el forEach y
        // TODAS las clínicas siguientes en la lista se quedaban sin sus
        // tablas (patients, doctors, etc.) hasta el próximo reinicio del
        // backend. Eso es lo que causaba el error intermitente
        // "relation ... does not exist" en el dashboard: dependía de en qué
        // orden se procesaran las organizaciones al arrancar.
        organizations.forEach { organization ->
            try {
                migrateTenantSchema(organization.schemaName)
            } catch (ex: Exception) {
                failedSchemas.add(organization.schemaName)
                println(
                    "ERROR migrando tenant '${organization.schemaName}' " +
                            "(org: ${organization.name}): ${ex.message}. " +
                            "Se continúa con los siguientes tenants."
                )
                ex.printStackTrace()
            }
        }

        if (failedSchemas.isNotEmpty()) {
            println(
                "ATENCIÓN: ${failedSchemas.size} schema(s) no pudieron " +
                        "migrarse y quedarán con tablas faltantes hasta que se " +
                        "corrijan: $failedSchemas. Puedes reintentar manualmente " +
                        "cada uno llamando a migrateTenantSchema(schemaName) o " +
                        "al endpoint POST /api/test/provision/{schema}."
            )
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