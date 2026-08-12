package com.healthmarketplace.backend.modules.billing.service

import com.fasterxml.jackson.databind.JsonNode
import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class MercadoPagoClient(properties: MercadoPagoProperties) {
    private val client = RestClient.builder()
        .baseUrl("https://api.mercadopago.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.accessToken}")
        .build()

    fun post(path: String, body: Any): JsonNode = client.post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode::class.java)
        ?: throw IllegalStateException("Mercado Pago no devolvio una respuesta")

    fun put(path: String, body: Any): JsonNode = client.put()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode::class.java)
        ?: throw IllegalStateException("Mercado Pago no devolvio una respuesta")

    fun get(path: String): JsonNode = client.get()
        .uri(path)
        .retrieve()
        .body(JsonNode::class.java)
        ?: throw IllegalStateException("Mercado Pago no devolvio una respuesta")
}
