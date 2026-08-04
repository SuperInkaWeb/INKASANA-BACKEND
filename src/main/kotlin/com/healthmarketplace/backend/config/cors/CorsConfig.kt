package com.healthmarketplace.backend.config.cors

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    // Lista separada por comas, ej: "http://localhost:5173,https://mi-tunel.trycloudflare.com"
    // Si la variable de entorno ALLOWED_ORIGINS no existe, cae en los defaults de localhost.
    @Value("\${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private val allowedOriginsRaw: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()

        config.allowedOrigins = allowedOriginsRaw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        config.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        )

        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        return source
    }
}