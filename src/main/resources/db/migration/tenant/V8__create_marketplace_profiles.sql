CREATE TABLE IF NOT EXISTS marketplace_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_type VARCHAR(30) NOT NULL,
    doctor_id UUID,
    organization_id UUID,
    display_name VARCHAR(180) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    headline VARCHAR(180),
    description TEXT,
    city VARCHAR(120),
    country VARCHAR(120),
    address VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(180),
    profile_image_url TEXT,
    cover_image_url TEXT,
    consultation_price NUMERIC(10,2),
    consultation_duration_minutes INT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_type
    ON marketplace_profiles(profile_type);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_status
    ON marketplace_profiles(status);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_published
    ON marketplace_profiles(is_published);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_doctor_id
    ON marketplace_profiles(doctor_id);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_slug
    ON marketplace_profiles(slug);