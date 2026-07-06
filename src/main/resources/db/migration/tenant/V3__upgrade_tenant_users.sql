ALTER TABLE tenant_users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50);

ALTER TABLE tenant_users
    ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

ALTER TABLE tenant_users
    ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;