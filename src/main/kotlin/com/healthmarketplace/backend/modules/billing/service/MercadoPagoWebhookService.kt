package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.billing.config.MercadoPagoProperties
import com.healthmarketplace.backend.modules.publicapi.patientportal.entity.PatientPortalProfile
import com.healthmarketplace.backend.modules.publicapi.patientportal.repository.PatientPortalProfileRepository
import com.healthmarketplace.backend.modules.tenant.appointment.entity.Appointment
import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import com.healthmarketplace.backend.modules.tenant.appointment.repository.AppointmentRepository
import com.healthmarketplace.backend.modules.tenant.patient.entity.Patient
import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import com.healthmarketplace.backend.modules.tenant.patient.repository.PatientRepository
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException
import com.mercadopago.webhook.WebhookSignatureValidator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MercadoPagoWebhookService(
    private val jdbcTemplate: JdbcTemplate,
    private val mercadoPagoClient: MercadoPagoClient,
    private val mercadoPagoProperties: MercadoPagoProperties,
    private val patientProfiles: PatientPortalProfileRepository,
    private val patients: PatientRepository,
    private val appointments: AppointmentRepository
) {
    @Transactional
    fun process(xSignature: String?, xRequestId: String?, dataId: String?, type: String?) {
        println("Mercado Pago webhook recibido: type=$type, paymentOrPreapprovalId=$dataId, requestId=$xRequestId")
        if (mercadoPagoProperties.webhookSecret.isBlank()) {
            println("ERROR webhook Mercado Pago: MERCADOPAGO_WEBHOOK_SECRET está vacía en Render")
            throw IllegalStateException("MERCADOPAGO_WEBHOOK_SECRET es obligatoria para recibir webhooks")
        }
        if (xSignature.isNullOrBlank() || xRequestId.isNullOrBlank() || dataId.isNullOrBlank()) {
            println("Webhook Mercado Pago ignorado: notificación sin firma o sin data.id")
            return
        }
        try {
            WebhookSignatureValidator.validate(xSignature, xRequestId, dataId, mercadoPagoProperties.webhookSecret)
        } catch (exception: MPInvalidWebhookSignatureException) {
            println("ERROR webhook Mercado Pago: firma inválida. Revisa que MERCADOPAGO_WEBHOOK_SECRET sea la clave secreta de la misma aplicación que generó el pago.")
            throw IllegalArgumentException("Firma de webhook de Mercado Pago invalida", exception)
        }
        val inserted = jdbcTemplate.update(
            "INSERT INTO mercadopago_webhook_events (mercadopago_event_id, event_type) VALUES (?, ?) ON CONFLICT (mercadopago_event_id) DO NOTHING",
            xRequestId, type ?: "unknown"
        )
        if (inserted == 0) return

        try {
            when (type) {
                "subscription_preapproval" -> synchronizeSubscription(dataId)
                "payment" -> synchronizeAppointmentPayment(dataId)
                else -> println("Mercado Pago webhook ignorado: tipo no manejado '$type'")
            }
        } catch (exception: Exception) {
            println("ERROR procesando webhook de Mercado Pago: ${exception.message}")
            exception.printStackTrace()
            throw exception
        }
    }

    private fun synchronizeSubscription(preapprovalId: String) {
        val subscription = mercadoPagoClient.get("/preapproval/$preapprovalId")
        val organizationId = subscription.path("external_reference").asText().let { reference ->
            runCatching { UUID.fromString(reference) }.getOrNull()
        } ?: return
        val status = when (subscription.path("status").asText().lowercase()) {
            "authorized" -> "ACTIVE"
            "cancelled" -> "CANCELED"
            "paused" -> "PAST_DUE"
            else -> "INCOMPLETE"
        }
        val nextPaymentDate = subscription.path("next_payment_date").asText().takeIf { it.isNotBlank() }
            ?.let { OffsetDateTime.parse(it).toLocalDateTime() }
        jdbcTemplate.update(
            """
            UPDATE subscriptions
            SET status = ?, mercadopago_preapproval_id = ?, mercadopago_payer_id = NULLIF(?, ''),
                mercadopago_payer_email = NULLIF(?, ''), current_period_end = ?,
                cancel_at_period_end = ?, updated_at = NOW()
            WHERE organization_id = ?
            """.trimIndent(),
            status, preapprovalId, subscription.path("payer_id").asText(),
            subscription.path("payer_email").asText(), nextPaymentDate, status == "CANCELED", organizationId
        )
    }

    private fun synchronizeAppointmentPayment(mercadoPagoPaymentId: String) {
        val payment = mercadoPagoClient.get("/v1/payments/$mercadoPagoPaymentId")
        val reference = payment.path("external_reference").asText()
        val status = payment.path("status").asText().lowercase()
        println("Pago consultado: id=$mercadoPagoPaymentId, status=$status, externalReference=$reference")
        val checkoutId = reference.removePrefix("appointment:").takeIf { reference.startsWith("appointment:") }
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (checkoutId == null) {
            println("El pago $mercadoPagoPaymentId no corresponde a una reserva de cita")
            synchronizeSubscriptionPayment(reference, payment, mercadoPagoPaymentId)
            return
        }
        val checkout = findAppointmentCheckout(checkoutId)
        if (checkout == null) {
            println("No se encontró la reserva de cita $checkoutId para el pago $mercadoPagoPaymentId")
            return
        }

        if (status != "approved") {
            println("Pago de cita $mercadoPagoPaymentId todavía no aprobado: $status")
            jdbcTemplate.update(
                "UPDATE appointment_payment_checkouts SET status = ?, updated_at = NOW() WHERE id = ?",
                status.uppercase(), checkout.id
            )
            return
        }
        if (checkout.appointmentId != null) {
            println("La reserva $checkoutId ya tiene la cita ${checkout.appointmentId}")
            return
        }

        val patientProfile = patientProfiles.findById(checkout.patientProfileId)
            .orElseThrow { IllegalStateException("Perfil de paciente no encontrado") }
        val appointmentId = try {
            TenantContext.setTenant(checkout.tenantSchema)
            val patient = findOrCreateTenantPatient(patientProfile)
            appointments.save(
                Appointment(
                    patientId = requireNotNull(patient.id), doctorId = checkout.doctorId,
                    tenantId = checkout.tenantSchema, date = checkout.date, time = checkout.time,
                    status = AppointmentStatus.PAID, price = checkout.amountCents.toBigDecimal().movePointLeft(2)
                )
            ).id ?: throw IllegalStateException("No se pudo crear la cita")
        } finally {
            TenantContext.clear()
        }

        val paidAt = payment.path("date_approved").asText().takeIf { it.isNotBlank() }
            ?.let { runCatching { OffsetDateTime.parse(it).toLocalDateTime() }.getOrNull() }
            ?: LocalDateTime.now()
        jdbcTemplate.update(
            """
            UPDATE appointment_payment_checkouts
            SET status = 'APPROVED', mercadopago_payment_id = ?, appointment_id = ?, paid_at = ?, updated_at = NOW()
            WHERE id = ?
            """.trimIndent(), mercadoPagoPaymentId, appointmentId, paidAt, checkout.id
        )
        saveBillingRecords(checkout, mercadoPagoPaymentId, paidAt)
        println("Cita pagada creada: appointmentId=$appointmentId, paymentId=$mercadoPagoPaymentId")
    }

    private fun synchronizeSubscriptionPayment(reference: String, payment: com.fasterxml.jackson.databind.JsonNode, paymentId: String) {
        val organizationId = runCatching { UUID.fromString(reference) }.getOrNull() ?: return
        if (payment.path("status").asText().lowercase() != "approved") return
        val amountCents = payment.path("transaction_amount").decimalValue()
            .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val currency = payment.path("currency_id").asText().ifBlank { mercadoPagoProperties.currency.uppercase() }
        val paidAt = payment.path("date_approved").asText().takeIf { it.isNotBlank() }
            ?.let { runCatching { OffsetDateTime.parse(it).toLocalDateTime() }.getOrNull() }
            ?: LocalDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO payments (organization_id, mercadopago_payment_id, amount_cents, currency, status, paid_at, purpose)
            VALUES (?, ?, ?, ?, 'PAID', ?, 'SUBSCRIPTION')
            ON CONFLICT (mercadopago_payment_id) DO UPDATE
            SET status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(), organizationId, paymentId, amountCents, currency, paidAt
        )
        jdbcTemplate.update(
            """
            INSERT INTO invoices (organization_id, stripe_invoice_id, invoice_number, amount_due_cents, amount_paid_cents, currency, status, paid_at)
            VALUES (?, ?, ?, ?, ?, ?, 'PAID', ?)
            ON CONFLICT (stripe_invoice_id) DO UPDATE
            SET amount_paid_cents = EXCLUDED.amount_paid_cents, status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(), organizationId, "mercadopago-payment-$paymentId", "MP-$paymentId", amountCents, amountCents, currency, paidAt
        )
    }

    private fun findOrCreateTenantPatient(profile: PatientPortalProfile): Patient {
        return patients.findByEmailIgnoreCase(profile.email).orElseGet {
            val fullName = listOfNotNull(profile.firstName, profile.lastName)
                .joinToString(" ") { it.trim() }
                .ifBlank { profile.email.substringBefore('@') }
            patients.save(Patient(fullName = fullName, identification = profile.dni, phone = profile.phone, email = profile.email, status = PatientStatus.ACTIVE))
        }
    }

    private fun saveBillingRecords(checkout: AppointmentCheckout, paymentId: String, paidAt: LocalDateTime) {
        jdbcTemplate.update(
            """
            INSERT INTO payments (organization_id, mercadopago_payment_id, amount_cents, currency, status, paid_at, purpose, appointment_checkout_id)
            VALUES (?, ?, ?, ?, 'PAID', ?, 'APPOINTMENT', ?)
            ON CONFLICT (mercadopago_payment_id) DO UPDATE
            SET status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(),
            checkout.organizationId, paymentId, checkout.amountCents, checkout.currency, paidAt, checkout.id
        )
        jdbcTemplate.update(
            """
            INSERT INTO invoices (organization_id, stripe_invoice_id, invoice_number, amount_due_cents, amount_paid_cents, currency, status, paid_at)
            VALUES (?, ?, ?, ?, ?, ?, 'PAID', ?)
            ON CONFLICT (stripe_invoice_id) DO UPDATE
            SET amount_paid_cents = EXCLUDED.amount_paid_cents, status = 'PAID', paid_at = EXCLUDED.paid_at, updated_at = NOW()
            """.trimIndent(),
            checkout.organizationId, "mercadopago-payment-$paymentId", "MP-$paymentId",
            checkout.amountCents, checkout.amountCents, checkout.currency, paidAt
        )
    }

    private fun findAppointmentCheckout(id: UUID): AppointmentCheckout? = jdbcTemplate.query(
        """
        SELECT id, organization_id, tenant_schema, patient_portal_profile_id, doctor_id, appointment_date, appointment_time,
               amount_cents, currency, appointment_id
        FROM appointment_payment_checkouts WHERE id = ?
        """.trimIndent(),
        { rs, _ -> AppointmentCheckout(
            rs.getObject("id", UUID::class.java), rs.getObject("organization_id", UUID::class.java), rs.getString("tenant_schema"),
            rs.getObject("patient_portal_profile_id", UUID::class.java), rs.getObject("doctor_id", UUID::class.java),
            rs.getObject("appointment_date", LocalDate::class.java), rs.getObject("appointment_time", LocalTime::class.java),
            rs.getLong("amount_cents"), rs.getString("currency"), rs.getObject("appointment_id", UUID::class.java)
        ) }, id
    ).firstOrNull()

    private data class AppointmentCheckout(
        val id: UUID, val organizationId: UUID, val tenantSchema: String, val patientProfileId: UUID, val doctorId: UUID,
        val date: LocalDate, val time: LocalTime, val amountCents: Long, val currency: String, val appointmentId: UUID?
    )
}
