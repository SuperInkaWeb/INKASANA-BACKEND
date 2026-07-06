package com.healthmarketplace.backend.modules.publicapi.auth.controller

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class Auth0TestController {

    @GetMapping("/me")
    fun me(jwt: Jwt): Map<String, Any> {

        return mapOf(
            "claims" to jwt.claims
        )
    }
}