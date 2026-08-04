package com.healthmarketplace.backend.modules.billing.controller

import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.CreateCheckoutSessionRequest
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.billing.service.BillingService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/billing")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
class BillingController(
    private val billingService: BillingService
) {
    @GetMapping("/subscription")
    fun getSubscription(@AuthenticationPrincipal jwt: Jwt): BillingSummaryResponse {
        return billingService.summary(UUID.fromString(jwt.getClaimAsString("orgId")))
    }

    @PostMapping("/checkout-session")
    fun createCheckoutSession(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateCheckoutSessionRequest
    ): RedirectUrlResponse {
        return billingService.checkout(
            UUID.fromString(jwt.getClaimAsString("orgId")),
            request.planCode
        )
    }

    @PostMapping("/subscription/cancel")
    fun cancelSubscription(@AuthenticationPrincipal jwt: Jwt): BillingSummaryResponse {
        return billingService.cancel(UUID.fromString(jwt.getClaimAsString("orgId")))
    }
}