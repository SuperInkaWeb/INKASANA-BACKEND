package com.healthmarketplace.backend.modules.publicapi.marketplace.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.healthmarketplace.backend.modules.billing.service.MercadoPagoClient
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.CreatePublicAppointmentCheckoutRequest
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.PublicAppointmentCheckoutResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.repository.GlobalMarketplaceProfileRepository
import com.healthmarketplace.backend.modules.tenant.agenda.service.SlotGeneratorService
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import org.springframework.stereotype.Service

@Service
class PublicAppointmentCheckoutService(
    private val marketplaceProfiles: GlobalMarketplaceProfileRepository,
    private val doctors: DoctorRepository,
    private val slots: SlotGeneratorService,
    private val mercadoPagoClient: MercadoPagoClient,
    private val mercadoPagoProperties: MercadoPagoProperties
) {
    fun checkout(doctorSlug: String, request: CreatePublicAppointmentCheckoutRequest): PublicAppointmentCheckoutResponse {
        val profile = marketplaceProfiles.findBySlug(doctorSlug.trim().lowercase())
            ?: throw BusinessException("Doctor no encontrado en el marketplace")
        if (profile.profileType != MarketplaceProfileType.DOCTOR || !profile.isPublished || profile.status != MarketplaceProfileStatus.PUBLISHED) throw BusinessException("Doctor no disponible para reservas")
        if (profile.sourceDoctorId != request.doctorId) throw BusinessException("El doctor seleccionado no coincide con el perfil")
        val (doctorName, price) = try {
            TenantContext.setTenant(profile.schemaName)
            val doctor = doctors.findById(request.doctorId).orElseThrow { BusinessException("Doctor no encontrado") }
            val amount = doctor.consultationPrice?.takeIf { it.signum() > 0 } ?: throw BusinessException("El doctor no tiene un precio de consulta configurado")
            val available = slots.generateSlots(request.doctorId, request.date, request.date).flatMap { it.slots }.any { it.available && it.startTime == request.time }
            if (!available) throw BusinessException("Ese horario ya no esta disponible")
            doctor.fullName to amount
        } finally { TenantContext.clear() }
        require(mercadoPagoProperties.webhookUrl.isNotBlank()) { "MERCADOPAGO_WEBHOOK_URL es obligatoria para iniciar el checkout" }
        val returnUrl = "${mercadoPagoProperties.frontendUrl}/marketplace/doctors/${profile.slug}"
        val response = mercadoPagoClient.post(
            "/checkout/preferences",
            mapOf(
                "items" to listOf(mapOf(
                    "title" to "Consulta con $doctorName",
                    "description" to "${request.date} a las ${request.time.toString().take(5)}",
                    "quantity" to 1,
                    "currency_id" to mercadoPagoProperties.currency.uppercase(),
                    "unit_price" to price
                )),
                "external_reference" to "appointment:${profile.schemaName}:${request.doctorId}:${request.date}:${request.time}",
                "back_urls" to mapOf("success" to "$returnUrl?payment=success", "failure" to "$returnUrl?payment=failure", "pending" to "$returnUrl?payment=pending"),
                "auto_return" to "approved",
                "notification_url" to mercadoPagoProperties.webhookUrl
            )
        )
        val checkoutUrl = response.path("sandbox_init_point").asText().ifBlank { response.path("init_point").asText() }
        require(checkoutUrl.isNotBlank()) { "Mercado Pago no devolvio el enlace de checkout" }
        return PublicAppointmentCheckoutResponse(checkoutUrl)
    }
}
