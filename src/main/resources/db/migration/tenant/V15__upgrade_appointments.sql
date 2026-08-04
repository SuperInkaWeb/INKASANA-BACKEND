-- La tabla ya existía en V1. Esta migración la lleva al modelo de agenda actual.
ALTER TABLE appointments
ALTER COLUMN appointment_date TYPE DATE USING appointment_date::date,
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS appointment_time TIME,
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS price NUMERIC(10, 2);

UPDATE appointments
SET appointment_time = COALESCE(appointment_time, '00:00:00'::time),
    tenant_id = COALESCE(tenant_id, current_schema()),
    status = CASE WHEN status = 'SCHEDULED' THEN 'PENDING' ELSE status END;

ALTER TABLE appointments
    ALTER COLUMN appointment_time SET NOT NULL,
ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date
    ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_date
    ON appointments(patient_id, appointment_date);
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_active_doctor_slot
    ON appointments(doctor_id, appointment_date, appointment_time)
    WHERE status NOT IN ('CANCELLED');