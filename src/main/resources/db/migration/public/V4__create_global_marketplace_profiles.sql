CREATE TABLE IF NOT EXISTS marketplace_profiles_global (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_profile_id UUID,
    source_doctor_id UUID,
    source_organization_id UUID,
    tenant_slug VARCHAR(120) NOT NULL,
    schema_name VARCHAR(120) NOT NULL,
    profile_type VARCHAR(30) NOT NULL,
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
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_type
    ON marketplace_profiles_global(profile_type);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_status
    ON marketplace_profiles_global(status);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_city
    ON marketplace_profiles_global(city);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_country
    ON marketplace_profiles_global(country);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_tenant_slug
    ON marketplace_profiles_global(tenant_slug);

CREATE INDEX IF NOT EXISTS idx_marketplace_profiles_global_source_profile_id
    ON marketplace_profiles_global(source_profile_id);