CREATE TABLE IF NOT EXISTS doctor_specialties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    specialty_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_doctor_specialties_doctor
    FOREIGN KEY (doctor_id)
    REFERENCES doctors(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_doctor_specialties_specialty
    FOREIGN KEY (specialty_id)
    REFERENCES public.global_specialties(id),

    CONSTRAINT uk_doctor_specialty UNIQUE (doctor_id, specialty_id)
    );

CREATE INDEX IF NOT EXISTS idx_doctor_specialties_doctor_id
    ON doctor_specialties(doctor_id);

CREATE INDEX IF NOT EXISTS idx_doctor_specialties_specialty_id
    ON doctor_specialties(specialty_id);