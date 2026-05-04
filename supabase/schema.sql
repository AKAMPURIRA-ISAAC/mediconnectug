-- MediConnectUG PostgreSQL Schema
-- Paste this entire file into Supabase SQL Editor and click Run
-- USERS
CREATE TABLE IF NOT EXISTS users (
  id              SERIAL PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  email           VARCHAR(150) UNIQUE NOT NULL,
  phone           VARCHAR(20),
  password_hash   TEXT NOT NULL,
  blood_type      VARCHAR(5),
  date_of_birth   DATE,
  address         TEXT,
  allergies       TEXT,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
-- DOCTORS
CREATE TABLE IF NOT EXISTS doctors (
  id                 SERIAL PRIMARY KEY,
  name               VARCHAR(100) NOT NULL,
  specialty          VARCHAR(100) NOT NULL,
  rating             NUMERIC(3,1) DEFAULT 4.5,
  review_count       INT DEFAULT 0,
  consultation_fee   INT DEFAULT 50000,
  experience_years   INT DEFAULT 5,
  is_online          BOOLEAN DEFAULT FALSE,
  created_at         TIMESTAMPTZ DEFAULT NOW()
);
-- APPOINTMENTS
CREATE TABLE IF NOT EXISTS appointments (
  id                 SERIAL PRIMARY KEY,
  user_id            INT REFERENCES users(id) ON DELETE CASCADE,
  doctor_id          INT REFERENCES doctors(id) ON DELETE SET NULL,
  appointment_date   DATE NOT NULL,
  appointment_time   VARCHAR(10) NOT NULL,
  type               VARCHAR(20) DEFAULT 'in_person',
  status             VARCHAR(20) DEFAULT 'upcoming',
  fee                INT DEFAULT 0,
  notes              TEXT,
  created_at         TIMESTAMPTZ DEFAULT NOW()
);
-- MEDICAL RECORDS
CREATE TABLE IF NOT EXISTS medical_records (
  id           SERIAL PRIMARY KEY,
  user_id      INT REFERENCES users(id) ON DELETE CASCADE,
  title        VARCHAR(200) NOT NULL,
  type         VARCHAR(30) DEFAULT 'report',
  date         DATE NOT NULL,
  doctor_name  VARCHAR(100),
  description  TEXT,
  file_url     TEXT,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);
-- PRESCRIPTIONS
CREATE TABLE IF NOT EXISTS prescriptions (
  id                  SERIAL PRIMARY KEY,
  user_id             INT REFERENCES users(id) ON DELETE CASCADE,
  medication_name     VARCHAR(150) NOT NULL,
  dosage              VARCHAR(100) NOT NULL,
  frequency           VARCHAR(100) NOT NULL,
  duration            VARCHAR(100) NOT NULL,
  doctor_name         VARCHAR(100) NOT NULL,
  issued_date         DATE NOT NULL,
  status              VARCHAR(20) DEFAULT 'active',
  refills_remaining   INT DEFAULT 0,
  instructions        TEXT,
  created_at          TIMESTAMPTZ DEFAULT NOW()
);
-- NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
  id           SERIAL PRIMARY KEY,
  user_id      INT REFERENCES users(id) ON DELETE CASCADE,
  title        VARCHAR(200) NOT NULL,
  message      TEXT NOT NULL,
  type         VARCHAR(30) DEFAULT 'alert',
  is_read      BOOLEAN DEFAULT FALSE,
  action_url   TEXT,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);
-- CHAT MESSAGES
CREATE TABLE IF NOT EXISTS chat_messages (
  id               SERIAL PRIMARY KEY,
  user_id          INT REFERENCES users(id) ON DELETE CASCADE,
  conversation_id  VARCHAR(50) NOT NULL,
  message          TEXT NOT NULL,
  is_user          BOOLEAN DEFAULT TRUE,
  ai_context       TEXT,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);
-- HEALTH TIPS
CREATE TABLE IF NOT EXISTS health_tips (
  id         SERIAL PRIMARY KEY,
  category   VARCHAR(50) NOT NULL,
  icon       VARCHAR(20) DEFAULT '[tip]',
  title      VARCHAR(200) NOT NULL,
  content    TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
-- SAMPLE DOCTORS
INSERT INTO doctors (name, specialty, rating, review_count, consultation_fee, experience_years, is_online) VALUES
  ('Dr. Sarah Nakamya',     'General Practitioner', 4.9, 312,  45000, 12, TRUE),
  ('Dr. James Okello',      'Cardiologist',         4.8, 245,  90000, 15, TRUE),
  ('Dr. Grace Atim',        'Paediatrician',        4.7, 198,  55000,  9, TRUE),
  ('Dr. Moses Wasswa',      'Dermatologist',        4.6, 167,  70000,  8, FALSE),
  ('Dr. Annet Nabirye',     'OB/GYN Specialist',    4.9, 423,  80000, 14, TRUE),
  ('Dr. Peter Mugisha',     'Psychiatrist',         4.7, 134,  75000, 11, FALSE),
  ('Dr. Faith Kiggundu',    'Internal Medicine',    4.8, 289,  65000, 13, TRUE),
  ('Dr. David Ssemwogerere','Pulmonologist',        4.6, 112,  85000, 10, FALSE),
  ('Dr. Lydia Nansubuga',   'Ophthalmologist',      4.7, 156,  60000,  7, FALSE),
  ('Dr. Robert Byaruhanga', 'General Surgeon',      4.8, 201, 100000, 16, FALSE)
ON CONFLICT DO NOTHING;
-- SAMPLE HEALTH TIPS
INSERT INTO health_tips (category, icon, title, content) VALUES
  ('hydration',    '[water]',   'Drink 8 Glasses of Water Daily',   'Water regulates body temperature, transports nutrients, and removes waste. Start your day with a glass of water before anything else. Dehydration by just 2% reduces concentration and physical performance significantly.'),
  ('sleep',        '[sleep]',   'Prioritise 7-9 Hours of Sleep',    'During sleep your body repairs tissues, synthesises proteins, and releases growth hormones. Chronic sleep deprivation raises risk of diabetes, heart disease, and depression. Keep a consistent sleep schedule - even on weekends.'),
  ('nutrition',    '[salad]',   'Eat 5 Colours of Vegetables Daily','Each vegetable colour represents different phytonutrients. Red (tomato) = lycopene. Orange (carrot) = beta-carotene. Green (spinach) = folate. Purple (eggplant) = anthocyanins. White (garlic) = allicin. Together they protect against cancer and heart disease.'),
  ('exercise',     '[run]',     'Walk 10,000 Steps Every Day',      '30 minutes of brisk walking daily reduces risk of heart disease by 35%, diabetes by 58%, and depression by 48%. You do not need a gym - walk to work, take the stairs, or walk for 10 minutes after each meal.'),
  ('mental_health','[breathe]', 'Practice 5-Minute Deep Breathing', 'Slow breathing activates the parasympathetic nervous system, reducing cortisol and blood pressure. Try box breathing: inhale 4 sec, hold 4, exhale 4, hold 4. Do this 5 times when stressed. It works within 90 seconds.'),
  ('nutrition',    '[sunrise]', 'Never Skip Breakfast',             'Breakfast breaks the overnight fast and stabilises blood sugar for the whole day. People who eat breakfast have better memory, concentration, and lower BMI. Include protein (eggs, beans) and fibre (whole grain) for sustained energy.'),
  ('hygiene',      '[soap]',    'Wash Hands for 20 Seconds',        'Handwashing removes 99% of bacteria and viruses. Use soap, scrub all surfaces including between fingers and under nails, for at least 20 seconds. Critical moments: before eating, after toilet, after touching animals, after coughing/sneezing.'),
  ('prevention',   '[sun]',     'Get 15 Minutes of Sunlight Daily', 'Sunlight triggers vitamin D synthesis in the skin. Vitamin D deficiency affects 1 billion people globally and is linked to depression, weak bones, and poor immunity. In Uganda, 7-10 AM sunlight is ideal - the UV index is lower and safer.'),
  ('nutrition',    '[beans]',   'Eat Beans 3 Times a Week',         'Beans are the most affordable protein source available. 1 cup of cooked beans provides 15g protein, 15g fibre, and significant iron, folate, and potassium. High bean consumption is associated with lower rates of heart disease and colon cancer.'),
  ('dental',       '[tooth]',   'Brush Teeth Twice Daily',          'Brush for 2 minutes with fluoride toothpaste after breakfast and before bed. Floss once daily to remove plaque between teeth. Poor oral health is linked to heart disease, diabetes complications, and preterm birth. Visit a dentist every 6 months.')
ON CONFLICT DO NOTHING;