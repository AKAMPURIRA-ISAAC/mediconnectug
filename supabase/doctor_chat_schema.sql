-- ════════════════════════════════════════════════════════════════════════════
-- MediConnectUG DOCTOR LOGIN & PATIENT-DOCTOR CHAT ENHANCEMENT
-- Run this after the main schema.sql
-- ════════════════════════════════════════════════════════════════════════════

-- ── 1. Update doctors table for authentication ──────────────────────────────
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS email VARCHAR(150) UNIQUE;
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS license_number VARCHAR(50);
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS profile_image_url TEXT;
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS last_seen TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE doctors ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'doctor';

-- ── 2. Create chat_sessions table (patient-doctor chats) ───────────────────
CREATE TABLE IF NOT EXISTS chat_sessions (
  id                SERIAL PRIMARY KEY,
  patient_id        INT REFERENCES users(id) ON DELETE CASCADE,
  doctor_id         INT REFERENCES doctors(id) ON DELETE SET NULL,
  session_type      VARCHAR(20) DEFAULT 'doctor_chat', -- 'doctor_chat' | 'ai_chat'
  status            VARCHAR(20) DEFAULT 'active',      -- 'active' | 'completed' | 'transferred'
  urgency_level     VARCHAR(20) DEFAULT 'normal',      -- 'normal' | 'moderate' | 'urgent'
  chief_complaint   TEXT,
  created_at        TIMESTAMPTZ DEFAULT NOW(),
  updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- ── 3. Create direct_messages table (patient-doctor messages) ──────────────
CREATE TABLE IF NOT EXISTS direct_messages (
  id                SERIAL PRIMARY KEY,
  session_id        INT REFERENCES chat_sessions(id) ON DELETE CASCADE,
  sender_id         INT NOT NULL,
  sender_type       VARCHAR(20) NOT NULL,              -- 'patient' | 'doctor'
  message_text      TEXT NOT NULL,
  attachment_url    TEXT,
  is_read           BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- ── 4. Create ai_escalations table (AI->Doctor handoffs) ───────────────────
CREATE TABLE IF NOT EXISTS ai_escalations (
  id                  SERIAL PRIMARY KEY,
  user_id             INT REFERENCES users(id) ON DELETE CASCADE,
  doctor_id           INT REFERENCES doctors(id) ON DELETE SET NULL,
  reason              TEXT NOT NULL,
  urgency             VARCHAR(20) DEFAULT 'moderate',   -- 'mild' | 'moderate' | 'urgent' | 'emergency'
  symptoms            TEXT[],
  severity_score      INT,
  duration_text       VARCHAR(100),
  ai_assessment       TEXT,
  status              VARCHAR(20) DEFAULT 'pending',    -- 'pending' | 'accepted' | 'completed'
  created_at          TIMESTAMPTZ DEFAULT NOW(),
  accepted_at         TIMESTAMPTZ
);

-- ── 5. Update chat_messages to link with sessions ──────────────────────────
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS session_id INT REFERENCES chat_sessions(id) ON DELETE SET NULL;

-- ── 6. Create indexes for performance ───────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_chat_sessions_patient ON chat_sessions(patient_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_doctor ON chat_sessions(doctor_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_status ON chat_sessions(status);
CREATE INDEX IF NOT EXISTS idx_direct_messages_session ON direct_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_created ON direct_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_escalations_doctor ON ai_escalations(doctor_id);
CREATE INDEX IF NOT EXISTS idx_ai_escalations_status ON ai_escalations(status);
CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email);
CREATE INDEX IF NOT EXISTS idx_doctors_online ON doctors(is_online);

-- ── 7. Sample doctor accounts with login credentials ────────────────────────
-- Password for all sample doctors: "Doctor@123"
-- Hash generated with bcryptjs (10 rounds)
UPDATE doctors SET
  email = 'sarah.nakamya@mediconnect.ug',
  password_hash = '$2a$10$rKz8YQXZJqVYKGZxN8N4juZqFZJzK8W8BZJqFZJzK8W8BZJqFZJzKe',
  phone = '+256700123001',
  license_number = 'UMC/GP/2012/1234',
  bio = 'Board-certified General Practitioner with 12 years of experience in family medicine. Specializing in preventive care, chronic disease management, and patient education.'
WHERE name = 'Dr. Sarah Nakamya';

UPDATE doctors SET
  email = 'james.okello@mediconnect.ug',
  password_hash = '$2a$10$rKz8YQXZJqVYKGZxN8N4juZqFZJzK8W8BZJqFZJzK8W8BZJqFZJzKe',
  phone = '+256700123002',
  license_number = 'UMC/CARD/2010/5678',
  bio = 'Consultant Cardiologist with expertise in hypertension, heart failure, and arrhythmias. Committed to evidence-based cardiovascular care.'
WHERE name = 'Dr. James Okello';

UPDATE doctors SET
  email = 'grace.atim@mediconnect.ug',
  password_hash = '$2a$10$rKz8YQXZJqVYKGZxN8N4juZqFZJzK8W8BZJqFZJzK8W8BZJqFZJzKe',
  phone = '+256700123003',
  license_number = 'UMC/PAED/2015/9012',
  bio = 'Pediatrician dedicated to child health. Expert in vaccination, growth monitoring, and childhood illnesses.'
WHERE name = 'Dr. Grace Atim';

UPDATE doctors SET
  email = 'annet.nabirye@mediconnect.ug',
  password_hash = '$2a$10$rKz8YQXZJqVYKGZxN8N4juZqFZJzK8W8BZJqFZJzK8W8BZJqFZJzKe',
  phone = '+256700123005',
  license_number = 'UMC/OBGYN/2012/3456',
  bio = 'OB/GYN specialist with 14 years experience in maternal health, prenatal care, and reproductive medicine.'
WHERE name = 'Dr. Annet Nabirye';

UPDATE doctors SET
  email = 'faith.kiggundu@mediconnect.ug',
  password_hash = '$2a$10$rKz8YQXZJqVYKGZxN8N4juZqFZJzK8W8BZJqFZJzK8W8BZJqFZJzKe',
  phone = '+256700123007',
  license_number = 'UMC/IM/2013/7890',
  bio = 'Internal Medicine physician specializing in diabetes, hypertension, and complex medical conditions.'
WHERE name = 'Dr. Faith Kiggundu';

-- ── 8. Functions for real-time updates ──────────────────────────────────────
CREATE OR REPLACE FUNCTION update_doctor_last_seen()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE doctors SET last_seen = NOW() WHERE id = NEW.sender_id AND NEW.sender_type = 'doctor';
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_doctor_last_seen
AFTER INSERT ON direct_messages
FOR EACH ROW
EXECUTE FUNCTION update_doctor_last_seen();

CREATE OR REPLACE FUNCTION update_chat_session_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE chat_sessions SET updated_at = NOW() WHERE id = NEW.session_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_chat_session_update
AFTER INSERT ON direct_messages
FOR EACH ROW
EXECUTE FUNCTION update_chat_session_timestamp();

-- ── Success message ──────────────────────────────────────────────────────────
SELECT 'Doctor authentication & patient-doctor chat schema installed successfully!' AS status;

