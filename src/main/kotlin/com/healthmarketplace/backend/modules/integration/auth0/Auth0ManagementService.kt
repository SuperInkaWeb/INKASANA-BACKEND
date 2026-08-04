package com.healthmarketplace.backend.modules.integration.auth0

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.auth0.Auth0ManagementProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom

@Service
class Auth0ManagementService(
    private val properties: Auth0ManagementProperties
) {

    private val restTemplate = RestTemplate()

    fun createUser(email: String, fullName: String): String {
        val token = getManagementToken()
        val password = generateTemporaryPassword()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

        val body = mapOf(
            "email" to email,
            "name" to fullName,
            "connection" to properties.connection,
            "password" to password,
            "email_verified" to false,
            "verify_email" to true
        )

        println("TOKEN = ${token.take(30)}...")
        println("BODY REQUEST = $body")



        return try {
            val response = restTemplate.postForEntity(
                "https://${properties.domain}/api/v2/users",
                HttpEntity(body, headers),
                Map::class.java
            )

            val auth0UserId = response.body?.get("user_id")?.toString()
                ?: throw BusinessException("Auth0 no devolvió user_id")

            sendPasswordResetEmail(email)

            auth0UserId
        } catch (ex: HttpClientErrorException.Conflict) {
            throw BusinessException("Ya existe un usuario Auth0 con ese correo")
        }
        catch (ex: HttpClientErrorException) {

            println("STATUS = ${ex.statusCode}")
            println("BODY = ${ex.responseBodyAsString}")
            ex.printStackTrace()

            throw BusinessException("Error creando usuario en Auth0: ${ex.responseBodyAsString}")
        }
    }

    private fun getManagementToken(): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        println("DOMAIN = ${properties.domain}")
        println("CLIENT_ID = ${properties.clientId}")
        println("AUDIENCE = ${properties.audience}")



        val body = mapOf(
            "client_id" to properties.clientId,
            "client_secret" to properties.clientSecret,
            "audience" to properties.audience,
            "grant_type" to "client_credentials"
        )

        val response = restTemplate.postForEntity(
            "https://${properties.domain}/oauth/token",
            HttpEntity(body, headers),
            Map::class.java
        )

        return response.body?.get("access_token")?.toString()
            ?: throw BusinessException("No se pudo obtener token de Auth0 Management API")
    }

    private fun sendPasswordResetEmail(email: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val body = mapOf(
            "client_id" to properties.clientId,
            "email" to email,
            "connection" to properties.connection
        )

        try {
            restTemplate.postForEntity(
                "https://${properties.domain}/dbconnections/change_password",
                HttpEntity(body, headers),
                String::class.java
            )
        } catch (ex: HttpClientErrorException) {
            throw BusinessException(
                "Usuario creado en Auth0, pero no se pudo enviar el correo para crear contraseña: ${ex.responseBodyAsString}"
            )
        }
    }

    private fun generateTemporaryPassword(): String {
        val chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*"
        val random = SecureRandom()

        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}