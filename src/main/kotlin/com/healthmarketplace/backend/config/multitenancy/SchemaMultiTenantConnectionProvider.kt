package com.healthmarketplace.backend.config.multitenancy

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

@Component
class SchemaMultiTenantConnectionProvider(
    private val dataSource: DataSource
) : MultiTenantConnectionProvider<String> {

    override fun getAnyConnection(): Connection {
        return dataSource.connection
    }

    override fun releaseAnyConnection(connection: Connection) {
        connection.close()
    }

    override fun getConnection(tenantIdentifier: String): Connection {
        val schema = sanitizeSchemaName(tenantIdentifier)

        val connection = getAnyConnection()
        connection.createStatement().use { statement ->
            statement.execute("""SET search_path TO "$schema", public""")
        }

        return connection
    }

    override fun releaseConnection(tenantIdentifier: String, connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("""SET search_path TO public""")
        }
        connection.close()
    }

    override fun supportsAggressiveRelease(): Boolean = false

    override fun isUnwrappableAs(unwrapType: Class<*>): Boolean = false

    override fun <T> unwrap(unwrapType: Class<T>): T? = null

    private fun sanitizeSchemaName(schemaName: String?): String {
        val schema = schemaName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: TenantContext.DEFAULT_TENANT

        require(schema.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            "Nombre de schema inválido: $schema"
        }

        return schema
    }
}