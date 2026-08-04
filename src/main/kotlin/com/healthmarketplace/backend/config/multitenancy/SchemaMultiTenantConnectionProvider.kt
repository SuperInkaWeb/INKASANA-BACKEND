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

        // Defensa extra: si por algún motivo esta conexión llega con una
        // transacción abortada pendiente (ejemplo= quedó así por un error previo
        // que no se limpió correctamente), el SET search_path de abajo se
        // ignoraría en silencio. Limpiamos antes de fijar el schema.
        if (!connection.autoCommit) {
            try {
                connection.rollback()
            } catch (ex: Exception) {
                // si no se puede limpiar, seguimos igual; el SET de abajo
                // fallará de forma visible en vez de silenciosa.
            }
        }

        connection.createStatement().use { statement ->
            statement.execute("""SET search_path TO "$schema", public""")
        }

        return connection
    }

    override fun releaseConnection(tenantIdentifier: String, connection: Connection) {
        var resetOk = false

        try {
            // Si la conexión quedó en un estado de transacción abortada por un
            // error previo (ejemplo= una query que falló), cualquier SET posterior
            // se ignora en silencio hasta hacer ROLLBACK. Sin este rollback,
            // la conexión vuelve al pool con el search_path viejo, y la
            // próxima request que la reciba fallará con "relation does not
            // exist" aunque el código intente fijar el schema correcto.
            if (!connection.autoCommit) {
                connection.rollback()
            }

            connection.createStatement().use { statement ->
                statement.execute("""SET search_path TO public""")
            }

            resetOk = true
        } catch (ex: Exception) {
            println(
                "ADVERTENCIA: no se pudo limpiar el search_path de una " +
                        "conexión tras usar el tenant '$tenantIdentifier'. " +
                        "Se descarta la conexión del pool en vez de reutilizarla " +
                        "'sucia' (motivo: ${ex.message})"
            )
        } finally {
            if (resetOk) {
                // Reset confirmado: es seguro devolverla al pool para su reuso.
                connection.close()
            } else {
                // No se pudo confirmar que el search_path quedó en "public".
                // Devolverla al pool "cerrándola" normalmente arriesga a que
                // el próximo tenant que la tome herede el schema equivocado
                // y vuelva a ver "relation does not exist". Se aborta la
                // conexión físicamente para que el pool la descarte en vez
                // de reciclarla.
                try {
                    connection.abort(Runnable::run)
                } catch (ex: Exception) {
                    try {
                        connection.close()
                    } catch (ignored: Exception) {
                        // no hay más nada que hacer con esta conexión
                    }
                }
            }
        }
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