package com.healthmarketplace.backend.modules.publicapi.health.controller

/*import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class TestController {

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal jwt: Jwt
    ): Map<String, Any?> {

        return mapOf(
            "sub" to jwt.subject,
            "email" to jwt.claims["email"],
            "name" to jwt.claims["name"],
            "claims" to jwt.claims
        )
    }
}*/