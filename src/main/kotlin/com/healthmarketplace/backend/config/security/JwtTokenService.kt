package com.healthmarketplace.backend.config.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtTokenService(
    @Value("\${app.jwt.secret}")
    private val secret: String,

    @Value("\${app.jwt.expiration-seconds:3600}")
    private val expirationSeconds: Long
) {

    fun createTenantToken(
        userId: UUID,
        orgId: UUID,
        email: String,
        role: String,
        schemaName: String
    ): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(expirationSeconds)

        val claims = JWTClaimsSet.Builder()
            .subject(userId.toString())
            .claim("orgId", orgId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("schema", schemaName)
            .claim("scope", "TENANT")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader(JWSAlgorithm.HS256),
            claims
        )

        signedJWT.sign(MACSigner(secret.toByteArray()))

        return signedJWT.serialize()
    }
}