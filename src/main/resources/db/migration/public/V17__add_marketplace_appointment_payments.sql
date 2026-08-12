ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIPTION',
    ADD COLUMN IF NOT EXISTS appointment_checkout_id UUID;

CREATE TABLE IF NOT EXISTS appointment_payment_checkouts (
                                                             id UUID PRIMARY KEY,
                                                             organization_id UUID NOT NULL REFERENCES organizations(id),
    tenant_schema VARCHAR(120) NOT NULL,
    patient_portal_profile_id UUID NOT NULL REFERENCES patient_portal_profiles(id),
    doctor_id UUID NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    mercadopago_payment_id VARCHAR(255) UNIQUE,
    appointment_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_appointment_payment_checkouts_organization
    ON appointment_payment_checkouts(organization_id);
