package com.healthmarketplace.backend.config.security

import com.healthmarketplace.backend.config.multitenancy.TenantFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthConverter: JwtAuthConverter,
    private val tenantFilter: TenantFilter,

    @Value("\${app.jwt.secret}")
    private val internalJwtSecret: String
) {

    @Bean
    @Order(1)
    fun auth0SecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher("/api/auth/**")
            .csrf { it.disable() }
            .cors { }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(auth0JwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthConverter)
                }
            }
            .build()
    }

    @Bean
    @Order(2)
    fun appSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/health",
                        "/api/platform/organizations/**",
                        "/api/public/auth/**",
                        "/api/public/doctor-registration/**",
                        "/api/public/marketplace/**",
                        "/api/public/media/**",
                        "/api/billing/webhook/mercadopago"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(internalJwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthConverter)
                }
            }
            .addFilterAfter(
                tenantFilter,
                BearerTokenAuthenticationFilter::class.java
            )
            .build()
    }
    @Bean
    fun auth0JwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder
            .withJwkSetUri("https://dev-i25syim5mvrwjpag.us.auth0.com/.well-known/jwks.json")
            .build()
    }
    @Bean
    fun internalJwtDecoder(): JwtDecoder {
        val secretKey = SecretKeySpec(
            internalJwtSecret.toByteArray(),
            "HmacSHA256"
        )

        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .build()
    }
}
