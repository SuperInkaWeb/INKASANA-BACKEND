INSERT INTO plans (
    code,
    name,
    price_cents,
    billing_period,
    max_doctors,
    max_appointments,
    is_active,
    stripe_product_id,
    stripe_price_id
)
VALUES
    (
        'STARTER',
        'Plan Esencial',
        1000,
        'MONTHLY',
        3,
        100,
        TRUE,
        'prod_UzMzdqiqbswamE',
        'price_1TzOFWK7vij9Koz26ZGng7Ab'
    ),
    (
        'PROFESSIONAL',
        'Plan Profesional',
        2500,
        'MONTHLY',
        15,
        1000,
        TRUE,
        'prod_UzN4L0o682qe1I',
        ' price_1TzOKXK7vij9Koz2utoddQaC'
    ),
    (
        'ENTERPRISE',
        'Plan Vip',
        6000,
        'MONTHLY',
        NULL,
        NULL,
        TRUE,
        'prod_UzN5h4UBd5Hcet',
        'price_1TzOLjK7vij9Koz2w58BtyFB'
    )
    ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
                              price_cents = EXCLUDED.price_cents,
                              billing_period = EXCLUDED.billing_period,
                              max_doctors = EXCLUDED.max_doctors,
                              max_appointments = EXCLUDED.max_appointments,
                              is_active = EXCLUDED.is_active,
                              stripe_product_id = EXCLUDED.stripe_product_id,
                              stripe_price_id = EXCLUDED.stripe_price_id,
                              updated_at = NOW();