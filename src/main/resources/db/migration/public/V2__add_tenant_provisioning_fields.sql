ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS schema_ready BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS schema_ready_at TIMESTAMP;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS provisioning_error TEXT;