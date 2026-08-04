ALTER TABLE marketplace_profiles_global
    ADD COLUMN IF NOT EXISTS carousel_image_url_1 TEXT,
    ADD COLUMN IF NOT EXISTS carousel_image_url_2 TEXT,
    ADD COLUMN IF NOT EXISTS page_color VARCHAR(20),
    ADD COLUMN IF NOT EXISTS button_color VARCHAR(20),
    ADD COLUMN IF NOT EXISTS subscription_color VARCHAR(20);
