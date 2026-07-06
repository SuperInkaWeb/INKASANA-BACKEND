package com.healthmarketplace.backend.config.auth0

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth0.management")
data class Auth0ManagementProperties(
    var domain: String = "",
    var clientId: String = "",
    var clientSecret: String = "",
    var audience: String = "",
    var connection: String = ""
)