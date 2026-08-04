package com.healthmarketplace.backend.modules.billing.controller

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
        @RequestBody payload: String,
        @RequestHeader("x-signature") signature: String,
        @RequestHeader("x-request-id") requestId: String,
        @RequestParam(name = "data.id", required = false) dataIdParam: String?
    ) {
        mercadoPagoWebhookService.process(payload, signature, requestId, dataIdParam)
    }
}