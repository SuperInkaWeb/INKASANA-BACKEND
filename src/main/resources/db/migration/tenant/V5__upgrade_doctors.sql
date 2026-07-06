ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS tenant_user_id UUID;

ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS license_number VARCHAR(80);

ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS bio TEXT;

ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS consultation_price NUMERIC(10, 2);

ALTER TABLE doctors
    ADD COLUMN IF NOT EXISTS consultation_duration_minutes INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS idx_doctors_license_number
    ON doctors(license_number)
    WHERE license_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_doctors_status
    ON doctors(status);

CREATE INDEX IF NOT EXISTS idx_doctors_specialty
    ON doctors(specialty);

CREATE INDEX IF NOT EXISTS idx_doctors_tenant_user_id
    ON doctors(tenant_user_id);