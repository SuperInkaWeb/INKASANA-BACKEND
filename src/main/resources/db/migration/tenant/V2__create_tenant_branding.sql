CREATE TABLE IF NOT EXISTS tenant_branding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_name VARCHAR(180) NOT NULL,
    slogan VARCHAR(255),
    primary_color VARCHAR(30) NOT NULL DEFAULT '#1677ff',
    secondary_color VARCHAR(30) NOT NULL DEFAULT '#001529',
    logo_url TEXT,
    favicon_url TEXT,
    contact_email VARCHAR(180),
    contact_phone VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(120),
    country VARCHAR(120),
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );