-- Minimal reference data for LOCAL development and the e2e suite: one
-- country, two specialties and a syndicate for each, so onboarding has
-- something to pick. Safe to run any number of times. Never run in production.
--
--   docker exec -i nemouw-db-1 psql -U postgres -d nemouw < apps/web/e2e/fixtures/dev-seed.sql

INSERT INTO countries (name, iso_code, currency_code, phone_code, region)
VALUES ('Lebanon', 'LB', 'LBP', '+961', 'Middle East')
ON CONFLICT (iso_code) DO NOTHING;

INSERT INTO specialties (code, name, label, label_fr, label_ar)
SELECT v.code, v.name, v.label, v.label_fr, v.label_ar
FROM (VALUES
  ('speech_therapy', 'speech_therapy', 'Speech therapy', 'Orthophonie', 'علاج النطق'),
  ('occupational_therapy', 'occupational_therapy', 'Occupational therapy', 'Ergothérapie', 'العلاج الوظيفي')
) AS v(code, name, label, label_fr, label_ar)
WHERE NOT EXISTS (SELECT 1 FROM specialties s WHERE s.code = v.code);

INSERT INTO syndicates (name, specialty_id, country_id, contact_email)
SELECT v.name, s.id, c.id, v.contact_email
FROM (VALUES
  ('Lebanese Syndicate of Speech Therapists', 'speech_therapy', 'speech@syndicate.example'),
  ('Lebanese Syndicate of Occupational Therapists', 'occupational_therapy', 'ot@syndicate.example')
) AS v(name, specialty_code, contact_email)
JOIN specialties s ON s.code = v.specialty_code
JOIN countries c ON c.iso_code = 'LB'
ON CONFLICT (country_id, specialty_id) DO NOTHING;
