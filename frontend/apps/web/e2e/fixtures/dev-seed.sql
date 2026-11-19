-- Minimal reference data for LOCAL development and the e2e suite: one
-- country, two specialties and a syndicate for each, so onboarding has
-- something to pick, and common conditions to treat. Safe to run any number
-- of times. Never run in production.
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

INSERT INTO conditions (code, name, label, label_fr, label_ar)
SELECT v.code, v.code, v.label, v.label_fr, v.label_ar
FROM (VALUES
  ('autism_spectrum_disorder', 'Autism spectrum disorder', 'Trouble du spectre de l''autisme', 'اضطراب طيف التوحد'),
  ('adhd', 'ADHD', 'TDAH', 'اضطراب فرط الحركة وتشتت الانتباه'),
  ('speech_language_delay', 'Speech and language delay', 'Retard de parole et de langage', 'تأخر النطق واللغة'),
  ('stuttering', 'Stuttering', 'Bégaiement', 'التأتأة'),
  ('dyslexia', 'Dyslexia', 'Dyslexie', 'عسر القراءة'),
  ('learning_difficulties', 'Learning difficulties', 'Troubles des apprentissages', 'صعوبات التعلم'),
  ('cerebral_palsy', 'Cerebral palsy', 'Paralysie cérébrale', 'الشلل الدماغي'),
  ('down_syndrome', 'Down syndrome', 'Trisomie 21', 'متلازمة داون'),
  ('developmental_delay', 'Developmental delay', 'Retard de développement', 'تأخر النمو'),
  ('sensory_processing', 'Sensory processing difficulties', 'Troubles du traitement sensoriel', 'صعوبات المعالجة الحسية'),
  ('dyspraxia', 'Dyspraxia', 'Dyspraxie', 'عسر الأداء الحركي'),
  ('aphasia', 'Aphasia', 'Aphasie', 'الحبسة الكلامية'),
  ('dysphagia', 'Swallowing difficulties', 'Troubles de la déglutition', 'صعوبات البلع'),
  ('hearing_loss', 'Hearing loss', 'Perte auditive', 'ضعف السمع')
) AS v(code, label, label_fr, label_ar)
ON CONFLICT (code) DO NOTHING;
