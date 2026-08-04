ALTER TABLE tenant_users
    --varchar es para cuantos caracteres me va permitir--
    ADD COLUMN IF NOT EXISTS country VARCHAR(120);