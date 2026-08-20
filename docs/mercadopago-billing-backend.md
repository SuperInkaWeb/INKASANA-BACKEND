# Facturación con Mercado Pago - INKASANA Backend

Esta guía describe la integración de facturación actualmente implementada en INKASANA. El proyecto usa **Mercado Pago**, no Stripe.

## Rutas disponibles

Las rutas de facturación de tenant requieren autenticación:

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/billing/subscription` | Obtiene el estado y el plan de la suscripción. |
| `GET` | `/api/billing/payments` | Obtiene el historial de pagos. |
| `POST` | `/api/billing/checkout-session` | Crea una preaprobación y devuelve el enlace de checkout. |
| `POST` | `/api/billing/subscription/cancel` | Cancela la suscripción activa o pendiente. |
| `POST` | `/api/billing/webhook/mercadopago` | Recibe eventos de Mercado Pago; no requiere JWT. |

El endpoint de webhook está permitido explícitamente en `SecurityConfig`. Las demás rutas se protegen con el JWT interno y se resuelven dentro del tenant correspondiente.

## Configuración

La configuración se carga desde variables de entorno:

```env
MERCADOPAGO_ACCESS_TOKEN=
MERCADOPAGO_WEBHOOK_SECRET=
MERCADOPAGO_WEBHOOK_URL=
MERCADOPAGO_TEST_PAYER_EMAIL=
MERCADOPAGO_CURRENCY=PEN
FRONTEND_URL=
```

- `MERCADOPAGO_ACCESS_TOKEN`: token de acceso de Mercado Pago. Usar credenciales de prueba en sandbox y credenciales productivas en producción.
- `MERCADOPAGO_WEBHOOK_SECRET`: secreto utilizado para validar las notificaciones cuando esté configurado.
- `MERCADOPAGO_WEBHOOK_URL`: URL pública del backend que recibe `/api/billing/webhook/mercadopago`.
- `MERCADOPAGO_TEST_PAYER_EMAIL`: correo del comprador de prueba; se usa en sandbox cuando está disponible.
- `MERCADOPAGO_CURRENCY`: moneda de cobro. La configuración actual usa `PEN` por defecto.
- `FRONTEND_URL`: URL del frontend para las redirecciones posteriores al checkout.

Las propiedades se encuentran en `modules/billing/config/MercadoPagoProperties.kt` y se configuran bajo el prefijo `app.mercadopago` de `application.yaml`.

## Flujo de suscripción

1. El frontend solicita `POST /api/billing/checkout-session` con el código del plan.
2. `BillingService` consulta el plan activo y verifica que tenga un precio mayor a cero.
3. Si existe una suscripción pendiente anterior, se cancela para evitar preaprobaciones duplicadas.
4. El backend crea una **preapproval** mensual en Mercado Pago con el monto del plan, el correo pagador, `external_reference` de la organización, `back_url` y `notification_url`.
5. El backend guarda la suscripción con estado `INCOMPLETE` y devuelve el enlace de checkout (`sandbox_init_point` o `init_point`).
6. Mercado Pago procesa la autorización y envía una notificación al webhook.
7. El webhook sincroniza el estado de la preaprobación y registra el pago aprobado.

La fuente de verdad del estado de pago es Mercado Pago y la notificación procesada por el backend, no la redirección del navegador a `FRONTEND_URL`.

## Webhooks y conciliación

`MercadoPagoWebhookController` recibe los eventos en la ruta pública de webhook. El servicio consulta los recursos de Mercado Pago necesarios y actualiza los registros locales de suscripciones y pagos.

Además del webhook, `BillingService` ejecuta una conciliación al consultar el resumen de suscripción o el historial de pagos. Esta conciliación consulta la preaprobación y los pagos aprobados para reducir el riesgo de que una notificación perdida deje el estado local desactualizado.

Los pagos se registran con un identificador único de Mercado Pago para que la operación sea idempotente. Las tablas de facturación conservan información de suscripción, pagos e invoices. Algunos nombres de columnas heredados, como `stripe_invoice_id`, permanecen por compatibilidad de esquema, aunque los valores actuales se generan como `mercadopago-payment-<id>`.

## Citas pagadas en marketplace

El webhook también procesa los pagos de citas públicas. Cuando el pago queda aprobado:

1. Se verifica el checkout de cita pendiente.
2. Se localiza o crea el paciente dentro del esquema tenant de la clínica.
3. Se crea la cita con estado `PAID`.
4. Se actualiza el checkout y se registran los movimientos de pago.

Esta operación evita crear una cita pagada antes de que Mercado Pago confirme el estado aprobado.

## Cancelación

La cancelación llama a Mercado Pago para cambiar el estado de la preaprobación a `cancelled`. Después se actualiza la suscripción local con estado `CANCELED` y `cancel_at_period_end = true`.

## Pruebas y operación

Para pruebas, usar el token sandbox y una cuenta compradora de prueba configurada en `MERCADOPAGO_TEST_PAYER_EMAIL`. El webhook debe apuntar a una URL accesible públicamente, incluyendo el despliegue actual en Render.

Antes de habilitar cobros reales, validar:

1. Creación de checkout de suscripción.
2. Llegada y procesamiento del webhook.
3. Actualización de resumen e historial de pagos.
4. Cancelación de una suscripción.
5. Checkout y creación de una cita pagada desde marketplace.
