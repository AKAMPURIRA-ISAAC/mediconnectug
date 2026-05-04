-- ================================================================
-- MediConnectUG: Enable Row Level Security on all tables
-- Run this in Supabase SQL Editor after the schema script
-- ================================================================
-- Enable RLS on every table
ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctors         ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments    ENABLE ROW LEVEL SECURITY;
ALTER TABLE medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescriptions   ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications   ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages   ENABLE ROW LEVEL SECURITY;
ALTER TABLE health_tips     ENABLE ROW LEVEL SECURITY;
-- ----------------------------------------------------------------
-- DOCTORS & HEALTH TIPS: public read (anyone can browse doctors/tips)
-- ----------------------------------------------------------------
CREATE POLICY "Public read doctors"
  ON doctors FOR SELECT USING (true);
CREATE POLICY "Public read health_tips"
  ON health_tips FOR SELECT USING (true);
-- ----------------------------------------------------------------
-- USERS: each user can only read/update their own row
-- Note: inserts are done by the backend (server role bypasses RLS)
-- ----------------------------------------------------------------
CREATE POLICY "Users read own row"
  ON users FOR SELECT
  USING (id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users update own row"
  ON users FOR UPDATE
  USING (id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Backend can insert users"
  ON users FOR INSERT WITH CHECK (true);
-- ----------------------------------------------------------------
-- APPOINTMENTS: users see only their own
-- ----------------------------------------------------------------
CREATE POLICY "Users read own appointments"
  ON appointments FOR SELECT
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users insert own appointments"
  ON appointments FOR INSERT
  WITH CHECK (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users update own appointments"
  ON appointments FOR UPDATE
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users delete own appointments"
  ON appointments FOR DELETE
  USING (user_id::text = current_setting('app.current_user_id', true));
-- ----------------------------------------------------------------
-- MEDICAL RECORDS
-- ----------------------------------------------------------------
CREATE POLICY "Users read own medical_records"
  ON medical_records FOR SELECT
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users insert own medical_records"
  ON medical_records FOR INSERT
  WITH CHECK (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users delete own medical_records"
  ON medical_records FOR DELETE
  USING (user_id::text = current_setting('app.current_user_id', true));
-- ----------------------------------------------------------------
-- PRESCRIPTIONS
-- ----------------------------------------------------------------
CREATE POLICY "Users read own prescriptions"
  ON prescriptions FOR SELECT
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users update own prescriptions"
  ON prescriptions FOR UPDATE
  USING (user_id::text = current_setting('app.current_user_id', true));
-- ----------------------------------------------------------------
-- NOTIFICATIONS
-- ----------------------------------------------------------------
CREATE POLICY "Users read own notifications"
  ON notifications FOR SELECT
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users update own notifications"
  ON notifications FOR UPDATE
  USING (user_id::text = current_setting('app.current_user_id', true));
-- ----------------------------------------------------------------
-- CHAT MESSAGES
-- ----------------------------------------------------------------
CREATE POLICY "Users read own chat_messages"
  ON chat_messages FOR SELECT
  USING (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users insert own chat_messages"
  ON chat_messages FOR INSERT
  WITH CHECK (user_id::text = current_setting('app.current_user_id', true));
CREATE POLICY "Users delete own chat_messages"
  ON chat_messages FOR DELETE
  USING (user_id::text = current_setting('app.current_user_id', true));
-- ----------------------------------------------------------------
-- Grant backend service role full access (bypasses RLS anyway,
-- but explicit grants keep things clean)
-- ----------------------------------------------------------------
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO service_role;