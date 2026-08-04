ALTER TABLE marketplace_profiles
    ADD COLUMN IF NOT EXISTS available_days VARCHAR(200),
    ADD COLUMN IF NOT EXISTS available_start_time VARCHAR(5),
    ADD COLUMN IF NOT EXISTS available_end_time VARCHAR(5);
