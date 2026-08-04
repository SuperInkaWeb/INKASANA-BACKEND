# Stripe Billing (modo prueba)

El frontend llama a estas rutas autenticadas:

```text
GET  /api/billing/subscription
POST /api/billing/checkout-session       { "planCode": "PROFESSIONAL" }
POST /api/billing/portal-session
POST /api/billing/webhook/stripe         (sin JWT; firma Stripe obligatoria)
```

Los endpoints autenticados deben obtener `orgId` del JWT y permitir solo los roles `OWNER` y `ADMIN`.
Nunca recibas, guardes ni envíes números de tarjeta, CVC o vencimientos: Checkout y Customer Portal los procesan en Stripe.

## 1. Dependencia y variables

En `pom.xml`, dentro de `<dependencies>` agrega:

```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>32.0.0</version>
</dependency>
```

En `src/main/resources/application.yaml`, debajo de `app:` agrega:

```yaml
  stripe:
    secret-key: ${STRIPE_SECRET_KEY}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    frontend-url: ${FRONTEND_URL:http://localhost:5173}
```

En tu archivo de entorno de desarrollo, sin subirlo a Git:

```env
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
FRONTEND_URL=http://localhost:5173
```

## 2. Migración

Crea `src/main/resources/db/migration/public/V9__create_billing_tables.sql`:

```sql
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS stripe_product_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_price_id VARCHAR(255) UNIQUE;

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    subscription_id UUID REFERENCES subscriptions(id),
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_charge_id VARCHAR(255) UNIQUE,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID REFERENCES payments(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    stripe_balance_transaction_id VARCHAR(255) UNIQUE,
    type VARCHAR(50) NOT NULL,
    amount_cents BIGINT NOT NULL,
    fee_cents BIGINT NOT NULL DEFAULT 0,
    net_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    available_on TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    subscription_id UUID REFERENCES subscriptions(id),
    stripe_invoice_id VARCHAR(255) NOT NULL UNIQUE,
    invoice_number VARCHAR(255),
    hosted_invoice_url TEXT,
    invoice_pdf_url TEXT,
    amount_due_cents BIGINT NOT NULL DEFAULT 0,
    amount_paid_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    due_date TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID REFERENCES payments(id),
    invoice_id UUID REFERENCES invoices(id),
    stripe_payment_intent_id VARCHAR(255),
    attempt_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    failure_code VARCHAR(255),
    failure_message TEXT,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (stripe_payment_intent_id, attempt_number)
);

CREATE TABLE IF NOT EXISTS stripe_webhook_events (
    stripe_event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_organization_id ON payments(organization_id);
CREATE INDEX IF NOT EXISTS idx_invoices_organization_id ON invoices(organization_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_organization_id ON subscriptions(organization_id);
```

Carga desde Stripe Dashboard los identificadores `price_...` en la tabla `plans`. Los `code` deben coincidir con el frontend: `STARTER`, `PROFESSIONAL` y `ENTERPRISE`.

## 3. Configuración Stripe

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/config/StripeProperties.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.stripe")
data class StripeProperties(
    val secretKey: String,
    val webhookSecret: String,
    val frontendUrl: String
)
```

En `src/main/kotlin/com/healthmarketplace/backend/BackendApplication.kt`, importa `StripeProperties` y deja la anotación así:

```kotlin
@EnableConfigurationProperties(
    Auth0ManagementProperties::class,
    StripeProperties::class
)
```

## 4. Contrato HTTP

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/dto/BillingDtos.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.dto

import jakarta.validation.constraints.Pattern
import java.time.LocalDateTime

data class CreateCheckoutSessionRequest(
    @field:Pattern(regexp = "STARTER|PROFESSIONAL|ENTERPRISE")
    val planCode: String
)

data class RedirectUrlResponse(val url: String)

data class BillingSummaryResponse(
    val status: String,
    val planName: String?,
    val currentPeriodEnd: LocalDateTime?,
    val cancelAtPeriodEnd: Boolean
)
```

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/controller/BillingController.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.controller

import com.healthmarketplace.backend.modules.billing.dto.*
import com.healthmarketplace.backend.modules.billing.service.BillingService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/billing")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
class BillingController(private val billingService: BillingService) {
    @GetMapping("/subscription")
    fun subscription(@AuthenticationPrincipal jwt: Jwt): BillingSummaryResponse =
        billingService.summary(UUID.fromString(jwt.getClaimAsString("orgId")))

    @PostMapping("/checkout-session")
    fun checkout(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateCheckoutSessionRequest): RedirectUrlResponse =
        billingService.checkout(UUID.fromString(jwt.getClaimAsString("orgId")), request.planCode)

    @PostMapping("/portal-session")
    fun portal(@AuthenticationPrincipal jwt: Jwt): RedirectUrlResponse =
        billingService.portal(UUID.fromString(jwt.getClaimAsString("orgId")))
}
```

El servicio debe: buscar el plan activo por `code`, crear o reutilizar `Customer` con `organization.email`, crear `Session` con `mode=SUBSCRIPTION`, `customer`, `lineItems(price=stripePriceId, quantity=1)`, `successUrl=$frontendUrl/billing?checkout=success` y `cancelUrl=$frontendUrl/billing?checkout=cancelled`. Para el portal usa `com.stripe.model.billingportal.Session.create(...)` con ese customer y `returnUrl=$frontendUrl/billing`.

## 5. Webhook

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/controller/StripeWebhookController.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.controller

import com.healthmarketplace.backend.modules.billing.service.StripeWebhookService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/billing/webhook")
class StripeWebhookController(private val webhookService: StripeWebhookService) {
    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    fun receive(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String
    ) = webhookService.process(payload, signature)
}
```

`StripeWebhookService` debe validar siempre la firma antes de tocar la base:

```kotlin
val event = Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret)
```

Luego inserta primero `event.id` en `stripe_webhook_events`; si ya existe, responde `200` sin reprocesar. Implementa estos eventos:

```text
checkout.session.completed       guarda stripe_customer_id
customer.subscription.created
customer.subscription.updated    sincroniza estado, periodo, cancel_at_period_end y price
customer.subscription.deleted    marca CANCELED
invoice.paid                     actualiza invoices y payments como PAID
invoice.payment_failed           actualiza invoices/payments y crea payment_attempts
charge.succeeded                 crea transactions si hay balance_transaction
```

No confíes en `success_url` para habilitar un plan: la fuente de verdad es el webhook verificado de Stripe.

## 6. Seguridad y pruebas

En `config/security/SecurityConfig.kt`, añade exactamente `"/api/billing/webhook/stripe"` a los `requestMatchers(...).permitAll()`. No abras las otras tres rutas.

En Stripe Dashboard, en **Test mode**, crea los tres productos/precios recurrentes mensuales, activa Customer Portal con `Payment methods`, `Cancel subscription`, `Switch plans` e `Invoice history`, y registra:

```text
https://TU-DOMINIO/api/billing/webhook/stripe
```

Para desarrollo local:

```bash
stripe listen --forward-to localhost:8080/api/billing/webhook/stripe
```

Usa la tarjeta de prueba `4242 4242 4242 4242`, fecha futura y CVC cualquiera.

## 7. Código de los servicios

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/service/BillingService.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.service

import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import com.healthmarketplace.backend.modules.billing.dto.BillingSummaryResponse
import com.healthmarketplace.backend.modules.billing.dto.RedirectUrlResponse
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.stripe.model.Customer
import com.stripe.model.billingportal.Session
import com.stripe.model.checkout.Session as CheckoutSession
import com.stripe.param.CustomerCreateParams
import com.stripe.param.billingportal.SessionCreateParams as PortalSessionCreateParams
import com.stripe.param.checkout.SessionCreateParams as CheckoutSessionCreateParams
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

@Service
class BillingService(
    private val jdbcTemplate: JdbcTemplate,
    private val organizationRepository: OrganizationRepository,
    private val stripeProperties: StripeProperties
) {
    fun summary(organizationId: UUID): BillingSummaryResponse {
        val sql = """
            SELECT s.status, p.name, s.current_period_end, s.cancel_at_period_end
            FROM subscriptions s
            JOIN plans p ON p.id = s.plan_id
            WHERE s.organization_id = ?
            ORDER BY s.updated_at DESC
            LIMIT 1
        """.trimIndent()

        val rows = jdbcTemplate.query(sql, { rs, _ ->
            BillingSummaryResponse(
                status = rs.getString("status"),
                planName = rs.getString("name"),
                currentPeriodEnd = rs.getTimestamp("current_period_end")?.toLocalDateTime(),
                cancelAtPeriodEnd = rs.getBoolean("cancel_at_period_end")
            )
        }, organizationId)

        return rows.firstOrNull()
            ?: BillingSummaryResponse("NONE", null, null, false)
    }

    @Transactional
    fun checkout(organizationId: UUID, planCode: String): RedirectUrlResponse {
        val plan = jdbcTemplate.query(
            "SELECT id, stripe_price_id FROM plans WHERE code = ? AND is_active = TRUE",
            { rs, _ -> Plan(UUID.fromString(rs.getString("id")), rs.getString("stripe_price_id")) },
            planCode
        ).firstOrNull() ?: throw IllegalArgumentException("El plan no existe o no está disponible")

        require(!plan.stripePriceId.isNullOrBlank()) {
            "El plan no tiene un precio de Stripe configurado"
        }

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalArgumentException("Organización no encontrada") }

        val customerId = jdbcTemplate.query(
            """SELECT stripe_customer_id FROM subscriptions
               WHERE organization_id = ? AND stripe_customer_id IS NOT NULL
               ORDER BY updated_at DESC LIMIT 1""",
            { rs, _ -> rs.getString("stripe_customer_id") },
            organizationId
        ).firstOrNull() ?: Customer.create(
            CustomerCreateParams.builder()
                .setName(organization.name)
                .setEmail(organization.email)
                .putMetadata("organizationId", organizationId.toString())
                .build()
        ).id

        jdbcTemplate.update(
            """UPDATE subscriptions SET stripe_customer_id = ?, updated_at = NOW()
               WHERE id = (SELECT id FROM subscriptions WHERE organization_id = ?
               ORDER BY updated_at DESC LIMIT 1)""",
            customerId, organizationId
        )

        if (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE organization_id = ?", Int::class.java, organizationId
            ) == 0) {
            jdbcTemplate.update(
                """INSERT INTO subscriptions (organization_id, plan_id, status, stripe_customer_id)
                   VALUES (?, ?, 'INCOMPLETE', ?)""",
                organizationId, plan.id, customerId
            )
        }

        val params = CheckoutSessionCreateParams.builder()
            .setMode(CheckoutSessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setSuccessUrl("${stripeProperties.frontendUrl}/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("${stripeProperties.frontendUrl}/billing?checkout=cancelled")
            .putMetadata("organizationId", organizationId.toString())
            .putMetadata("planCode", planCode)
            .addLineItem(
                CheckoutSessionCreateParams.LineItem.builder()
                    .setPrice(plan.stripePriceId)
                    .setQuantity(1L)
                    .build()
            )
            .build()

        return RedirectUrlResponse(requireNotNull(CheckoutSession.create(params).url))
    }

    fun portal(organizationId: UUID): RedirectUrlResponse {
        val customerId = jdbcTemplate.query(
            """SELECT stripe_customer_id FROM subscriptions
               WHERE organization_id = ? AND stripe_customer_id IS NOT NULL
               ORDER BY updated_at DESC LIMIT 1""",
            { rs, _ -> rs.getString("stripe_customer_id") },
            organizationId
        ).firstOrNull() ?: throw IllegalStateException("La organización no tiene una cuenta de facturación")

        val params = PortalSessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl("${stripeProperties.frontendUrl}/billing")
            .build()

        return RedirectUrlResponse(Session.create(params).url)
    }

    private data class Plan(val id: UUID, val stripePriceId: String?)
}
```

Crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/service/StripeWebhookService.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmarketplace.backend.modules.billing.config.StripeProperties
import com.stripe.net.Webhook
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class StripeWebhookService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val stripeProperties: StripeProperties
) {
    @Transactional
    fun process(payload: String, signature: String) {
        val event = Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret)
        val inserted = jdbcTemplate.update(
            """INSERT INTO stripe_webhook_events (stripe_event_id, event_type)
               VALUES (?, ?) ON CONFLICT (stripe_event_id) DO NOTHING""",
            event.id, event.type
        )
        if (inserted == 0) return

        val data = objectMapper.readTree(payload).path("data").path("object")
        when (event.type) {
            "checkout.session.completed" -> saveCheckout(data)
            "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" -> saveSubscription(data)
            "invoice.paid", "invoice.payment_failed" -> saveInvoice(data, event.type == "invoice.paid")
            "charge.succeeded" -> saveTransaction(data)
        }
    }

    private fun saveCheckout(session: JsonNode) {
        val metadata = session.path("metadata")
        val organizationId = metadata.path("organizationId").asText().takeIf { it.isNotBlank() } ?: return
        val planCode = metadata.path("planCode").asText().takeIf { it.isNotBlank() } ?: return
        val customerId = session.path("customer").asText()
        val stripeSubscriptionId = session.path("subscription").asText()

        jdbcTemplate.update(
            """UPDATE subscriptions SET stripe_customer_id = ?, stripe_subscription_id = NULLIF(?, ''),
               status = 'INCOMPLETE', updated_at = NOW()
               WHERE organization_id = ? AND plan_id = (SELECT id FROM plans WHERE code = ?)""",
            customerId, stripeSubscriptionId, UUID.fromString(organizationId), planCode
        )
    }

    private fun saveSubscription(subscription: JsonNode) {
        val customerId = subscription.path("customer").asText()
        val organizationId = organizationIdForCustomer(customerId) ?: return
        val stripeSubscriptionId = subscription.path("id").asText()
        val status = subscription.path("status").asText().uppercase()
        val priceId = subscription.path("items").path("data").path(0).path("price").path("id").asText()
        val planId = jdbcTemplate.query(
            "SELECT id FROM plans WHERE stripe_price_id = ?", { rs, _ -> UUID.fromString(rs.getString("id")) }, priceId
        ).firstOrNull() ?: return

        val updated = jdbcTemplate.update(
            """UPDATE subscriptions SET plan_id = ?, status = ?, stripe_customer_id = ?,
               current_period_start = ?, current_period_end = ?, cancel_at_period_end = ?, updated_at = NOW()
               WHERE stripe_subscription_id = ?""",
            planId, status, customerId, timestamp(subscription.path("current_period_start")),
            timestamp(subscription.path("current_period_end")), subscription.path("cancel_at_period_end").asBoolean(),
            stripeSubscriptionId
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """INSERT INTO subscriptions (organization_id, plan_id, status, stripe_customer_id,
                   stripe_subscription_id, current_period_start, current_period_end, cancel_at_period_end)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                organizationId, planId, status, customerId, stripeSubscriptionId,
                timestamp(subscription.path("current_period_start")), timestamp(subscription.path("current_period_end")),
                subscription.path("cancel_at_period_end").asBoolean()
            )
        }
    }

    private fun saveInvoice(invoice: JsonNode, paid: Boolean) {
        val customerId = invoice.path("customer").asText()
        val organizationId = organizationIdForCustomer(customerId) ?: return
        val subscriptionId = jdbcTemplate.query(
            "SELECT id FROM subscriptions WHERE stripe_customer_id = ? ORDER BY updated_at DESC LIMIT 1",
            { rs, _ -> UUID.fromString(rs.getString("id")) }, customerId
        ).firstOrNull()
        val stripeInvoiceId = invoice.path("id").asText()
        val status = invoice.path("status").asText().uppercase()

        jdbcTemplate.update(
            """INSERT INTO invoices (organization_id, subscription_id, stripe_invoice_id, invoice_number,
               hosted_invoice_url, invoice_pdf_url, amount_due_cents, amount_paid_cents, currency, status, due_date, paid_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT (stripe_invoice_id) DO UPDATE SET amount_paid_cents = EXCLUDED.amount_paid_cents,
               status = EXCLUDED.status, paid_at = EXCLUDED.paid_at, updated_at = NOW()""",
            organizationId, subscriptionId, stripeInvoiceId, nullableText(invoice, "number"),
            nullableText(invoice, "hosted_invoice_url"), nullableText(invoice, "invoice_pdf"),
            invoice.path("amount_due").asLong(), invoice.path("amount_paid").asLong(), invoice.path("currency").asText(),
            status, timestamp(invoice.path("due_date")), if (paid) Timestamp.from(Instant.now()) else null
        )

        val paymentIntentId = nullableText(invoice, "payment_intent") ?: return
        jdbcTemplate.update(
            """INSERT INTO payments (organization_id, subscription_id, stripe_payment_intent_id, amount_cents,
               currency, status, paid_at) VALUES (?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT (stripe_payment_intent_id) DO UPDATE SET status = EXCLUDED.status,
               paid_at = EXCLUDED.paid_at, updated_at = NOW()""",
            organizationId, subscriptionId, paymentIntentId, invoice.path("amount_due").asLong(),
            invoice.path("currency").asText(), if (paid) "PAID" else "FAILED",
            if (paid) Timestamp.from(Instant.now()) else null
        )
        if (!paid) recordFailedAttempt(paymentIntentId, stripeInvoiceId)
    }

    private fun recordFailedAttempt(paymentIntentId: String, stripeInvoiceId: String) {
        val paymentId = jdbcTemplate.query(
            "SELECT id FROM payments WHERE stripe_payment_intent_id = ?", { rs, _ -> UUID.fromString(rs.getString("id")) }, paymentIntentId
        ).firstOrNull() ?: return
        val invoiceId = jdbcTemplate.query(
            "SELECT id FROM invoices WHERE stripe_invoice_id = ?", { rs, _ -> UUID.fromString(rs.getString("id")) }, stripeInvoiceId
        ).firstOrNull()
        jdbcTemplate.update(
            """INSERT INTO payment_attempts (payment_id, invoice_id, stripe_payment_intent_id, attempt_number, status)
               VALUES (?, ?, ?, (SELECT COUNT(*) + 1 FROM payment_attempts WHERE stripe_payment_intent_id = ?), 'FAILED')""",
            paymentId, invoiceId, paymentIntentId, paymentIntentId
        )
    }

    private fun saveTransaction(charge: JsonNode) {
        val paymentIntentId = nullableText(charge, "payment_intent") ?: return
        val payment = jdbcTemplate.query(
            "SELECT id, organization_id FROM payments WHERE stripe_payment_intent_id = ?",
            { rs, _ -> UUID.fromString(rs.getString("id")) to UUID.fromString(rs.getString("organization_id")) }, paymentIntentId
        ).firstOrNull() ?: return
        val balanceTransactionId = nullableText(charge, "balance_transaction") ?: return
        jdbcTemplate.update(
            """INSERT INTO transactions (payment_id, organization_id, stripe_balance_transaction_id, type,
               amount_cents, fee_cents, net_cents, currency, status)
               VALUES (?, ?, ?, 'CHARGE', ?, 0, ?, ?, 'AVAILABLE') ON CONFLICT (stripe_balance_transaction_id) DO NOTHING""",
            payment.first, payment.second, balanceTransactionId, charge.path("amount").asLong(),
            charge.path("amount").asLong(), charge.path("currency").asText()
        )
    }

    private fun organizationIdForCustomer(customerId: String): UUID? = jdbcTemplate.query(
        "SELECT organization_id FROM subscriptions WHERE stripe_customer_id = ? ORDER BY updated_at DESC LIMIT 1",
        { rs, _ -> UUID.fromString(rs.getString("organization_id")) }, customerId
    ).firstOrNull()

    private fun timestamp(node: JsonNode): Timestamp? =
        node.takeIf { it.isNumber }?.asLong()?.let { Timestamp.from(Instant.ofEpochSecond(it)) }

    private fun nullableText(node: JsonNode, field: String): String? =
        node.path(field).asText().takeIf { it.isNotBlank() }
}
```

Finalmente, crea `src/main/kotlin/com/healthmarketplace/backend/modules/billing/config/StripeConfig.kt`:

```kotlin
package com.healthmarketplace.backend.modules.billing.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class StripeConfig(private val stripeProperties: StripeProperties) {
    @PostConstruct
    fun configure() {
        Stripe.apiKey = stripeProperties.secretKey
    }
}
```
