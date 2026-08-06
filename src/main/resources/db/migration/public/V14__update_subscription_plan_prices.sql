UPDATE plans
SET
    name = CASE code
               WHEN 'STARTER' THEN 'Plan Esencial'
               WHEN 'PROFESSIONAL' THEN 'Plan Profesional'
               WHEN 'ENTERPRISE' THEN 'Plan Vip'
        END,
    price_cents = CASE code
                      WHEN 'STARTER' THEN 0
                      WHEN 'PROFESSIONAL' THEN 5000
                      WHEN 'ENTERPRISE' THEN 14000
        END,
    updated_at = NOW()
WHERE code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
