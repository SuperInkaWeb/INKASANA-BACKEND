ALTER TABLE global_specialties
    ADD COLUMN IF NOT EXISTS slug VARCHAR(140);

ALTER TABLE global_specialties
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE global_specialties
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

UPDATE global_specialties
SET slug = LOWER(
        REPLACE(
                REPLACE(
                        REPLACE(
                                REPLACE(
                                        REPLACE(name, 'á', 'a'),
                                        'é', 'e'),
                                'í', 'i'),
                        'ó', 'o'),
                'ú', 'u')
           )
WHERE slug IS NULL;

UPDATE global_specialties
SET slug = REPLACE(slug, ' ', '-')
WHERE slug IS NOT NULL;

ALTER TABLE global_specialties
    ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_global_specialties_slug
    ON global_specialties(slug);

ALTER TABLE global_specialties
    ADD CONSTRAINT chk_global_specialties_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));

CREATE INDEX IF NOT EXISTS idx_global_specialties_status
    ON global_specialties(status);

CREATE INDEX IF NOT EXISTS idx_global_specialties_name
    ON global_specialties(name);

INSERT INTO global_specialties (
    name,
    slug,
    description,
    status
)
VALUES
    ('Medicina General', 'medicina-general', 'Atención médica primaria e integral.', 'ACTIVE'),
    ('Pediatría', 'pediatria', 'Atención médica de niños y adolescentes.', 'ACTIVE'),
    ('Ginecología', 'ginecologia', 'Salud ginecológica y reproductiva.', 'ACTIVE'),
    ('Cardiología', 'cardiologia', 'Diagnóstico y tratamiento de enfermedades del corazón.', 'ACTIVE'),
    ('Dermatología', 'dermatologia', 'Diagnóstico y tratamiento de enfermedades de la piel.', 'ACTIVE'),
    ('Traumatología', 'traumatologia', 'Lesiones musculoesqueléticas.', 'ACTIVE'),
    ('Odontología', 'odontologia', 'Salud oral y dental.', 'ACTIVE'),
    ('Psicología', 'psicologia', 'Atención psicológica y salud mental.', 'ACTIVE'),
    ('Psiquiatría', 'psiquiatria', 'Tratamiento médico de trastornos mentales.', 'ACTIVE'),
    ('Neurología', 'neurologia', 'Diagnóstico y tratamiento de enfermedades neurológicas.', 'ACTIVE'),
    ('Oftalmología', 'oftalmologia', 'Salud visual y enfermedades oculares.', 'ACTIVE'),
    ('Otorrinolaringología', 'otorrinolaringologia', 'Oído, nariz y garganta.', 'ACTIVE'),
    ('Endocrinología', 'endocrinologia', 'Hormonas y metabolismo.', 'ACTIVE'),
    ('Nutrición', 'nutricion', 'Planificación y evaluación nutricional.', 'ACTIVE'),
    ('Fisioterapia', 'fisioterapia', 'Rehabilitación física.', 'ACTIVE')
    ON CONFLICT (slug) DO NOTHING;