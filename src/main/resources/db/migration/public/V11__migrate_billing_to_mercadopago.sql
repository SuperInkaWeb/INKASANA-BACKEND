-- Columnas nuevas para Mercado Pago (las de stripe_* se dejan intactas,
-- no se borran datos históricos; puedes limpiarlas después si ya no las necesitas)
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS mercadopago_plan_id VARCHAR(255);

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS mercadopago_preapproval_id VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS mercadopago_payer_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mercadopago_payer_email VARCHAR(255);

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS mercadopago_payment_id VARCHAR(255) UNIQUE;

CREATE TABLE IF NOT EXISTS mercadopago_webhook_events (
                                                          mercadopago_event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
    );