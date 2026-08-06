package com.healthmarketplace.backend.modules.billing.controller

import com.healthmarketplace.backend.modules.billing.service.StripeWebhookService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/billing/webhook")
class StripeWebhookController(private val stripeWebhookService: StripeWebhookService) {
    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    fun receive(@RequestBody payload: String, @RequestHeader("Stripe-Signature") signature: String) =
        stripeWebhookService.process(payload, signature)
}
