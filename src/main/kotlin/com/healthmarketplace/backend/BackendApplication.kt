package com.healthmarketplace.backend

import com.healthmarketplace.backend.config.auth0.Auth0ManagementProperties
import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
	Auth0ManagementProperties::class,
	StripeProperties::class
)
class BackendApplication

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}
