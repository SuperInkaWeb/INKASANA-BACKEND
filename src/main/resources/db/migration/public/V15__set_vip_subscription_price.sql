UPDATE plans
SET price_cents = 15000,
    updated_at = NOW()
WHERE code = 'ENTERPRISE';
