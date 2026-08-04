-- Horarios recurrentes semanales del doctor (pueden existir varios
-- bloques por día, ej: mañana 08:00-12:00 y tarde 14:00-18:00).
CREATE TABLE IF NOT EXISTS doctor_availability (
                                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL, -- MONDAY, TUESDAY, ... SUNDAY (java.time.DayOfWeek)
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_doctor_availability_doctor
    FOREIGN KEY (doctor_id)
    REFERENCES doctors(id)
    ON DELETE CASCADE,

    CONSTRAINT chk_doctor_availability_time_order
    CHECK (end_time > start_time)
    );

CREATE INDEX IF NOT EXISTS idx_doctor_availability_doctor_id
    ON doctor_availability(doctor_id);

CREATE INDEX IF NOT EXISTS idx_doctor_availability_doctor_day
    ON doctor_availability(doctor_id, day_of_week);


-- Excepciones puntuales a una fecha concreta:
--   UNAVAILABLE -> el doctor NO atiende ese día (vacaciones, feriado, etc.)
--   EXTRA       -> bloque adicional de atención ese día, fuera de su
--                   horario recurrente habitual (cubre guardias, horario
--                   especial, etc.)
CREATE TABLE IF NOT EXISTS availability_exceptions (
                                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL,
    exception_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL, -- UNAVAILABLE | EXTRA
    start_time TIME, -- NULL cuando type = UNAVAILABLE (bloquea el día completo)
    end_time TIME,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_availability_exceptions_doctor
    FOREIGN KEY (doctor_id)
    REFERENCES doctors(id)
    ON DELETE CASCADE,

    CONSTRAINT chk_availability_exceptions_type_time
    CHECK (
(type = 'UNAVAILABLE' AND start_time IS NULL AND end_time IS NULL)
    OR
(type = 'EXTRA' AND start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)
    )
    );

CREATE INDEX IF NOT EXISTS idx_availability_exceptions_doctor_id
    ON availability_exceptions(doctor_id);

CREATE INDEX IF NOT EXISTS idx_availability_exceptions_date
    ON availability_exceptions(exception_date);

CREATE INDEX IF NOT EXISTS idx_availability_exceptions_doctor_date
    ON availability_exceptions(doctor_id, exception_date);
