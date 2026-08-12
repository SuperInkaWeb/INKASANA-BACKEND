package com.healthmarketplace.backend.modules.billing.controller

import com.fasterxml.jackson.databind.JsonNode
import com.healthmarketplace.backend.modules.billing.service.MercadoPagoWebhookService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/billing/webhook")
class MercadoPagoWebhookController(
    private val mercadoPagoWebhookService: MercadoPagoWebhookService
) {
    @PostMapping("/mercadopago")
    @ResponseStatus(HttpStatus.OK)
    fun receive(
        @RequestHeader("x-signature", required = false) xSignature: String?,
        @RequestHeader("x-request-id", required = false) xRequestId: String?,
        @RequestParam("data.id", required = false) dataId: String?,
        @RequestBody(required = false) payload: JsonNode?
    ) {
        val eventType = payload?.path("type")?.asText().orEmpty()
            .ifBlank { payload?.path("action")?.asText()?.substringBefore('.') ?: "" }
        mercadoPagoWebhookService.process(
            xSignature = xSignature,
            xRequestId = xRequestId,
            dataId = dataId ?: payload?.path("data")?.path("id")?.asText(),
            type = eventType
        )
    }
}
