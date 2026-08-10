ALTER TABLE public.patient_portal_profiles
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50);