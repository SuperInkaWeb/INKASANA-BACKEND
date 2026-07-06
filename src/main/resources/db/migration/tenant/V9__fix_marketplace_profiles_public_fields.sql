ALTER TABLE marketplace_profiles
    ADD COLUMN IF NOT EXISTS address VARCHAR(255);

ALTER TABLE marketplace_profiles
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50);

ALTER TABLE marketplace_profiles
    ADD COLUMN IF NOT EXISTS email VARCHAR(180);

ALTER TABLE marketplace_profiles
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;