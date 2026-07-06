package com.healthmarketplace.backend.modules.publicapi.auth.controller

import com.healthmarketplace.backend.modules.publicapi.auth.dto.AuthMeResponse
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthMeController {

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal jwt: Jwt
    ): AuthMeResponse {

        return AuthMeResponse(
            userId = jwt.subject,
            orgId = jwt.getClaimAsString("orgId"),
            email = jwt.getClaimAsString("email"),
            role = jwt.getClaimAsString("role"),
            scope = jwt.getClaimAsString("scope"),
            schema = jwt.getClaimAsString("schema")
        )
    }
}