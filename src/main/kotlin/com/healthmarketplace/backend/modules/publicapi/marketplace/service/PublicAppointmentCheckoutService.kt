package com.healthmarketplace.backend.modules.publicapi.marketplace.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.CreatePublicAppointmentCheckoutRequest
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.PublicAppointmentCheckoutResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.repository.GlobalMarketplaceProfileRepository
import com.healthmarketplace.backend.modules.tenant.agenda.service.SlotGeneratorService
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.stereotype.Service
import java.math.RoundingMode

@Service
class PublicAppointmentCheckoutService(
    private val marketplaceProfiles: GlobalMarketplaceProfileRepository,
    private val doctors: DoctorRepository,
    private val slots: SlotGeneratorService,
    private val stripeProperties: StripeProperties
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
            if (!available) throw BusinessException("Ese horario ya no está disponible")
            doctor.fullName to amount
        } finally { TenantContext.clear() }
        val amountCents = price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val returnUrl = "${stripeProperties.frontendUrl}/marketplace/doctors/${profile.slug}"
        val params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("$returnUrl?payment=success&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("$returnUrl?payment=cancelled")
            .putMetadata("doctorId", request.doctorId.toString()).putMetadata("date", request.date.toString()).putMetadata("time", request.time.toString())
            .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency(stripeProperties.currency).setUnitAmount(amountCents).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Consulta con $doctorName").setDescription("${request.date} a las ${request.time.toString().take(5)}").build()).build()).build())
            .build()
        return PublicAppointmentCheckoutResponse(Session.create(params).url)
    }
}
