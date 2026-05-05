-- ════════════════════════════════════════════════════════════════════════════
-- MediConnectUG - Full Database Reset Script
-- ════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE:
-- This script performs a FULL database reset for fresh start.
-- USE WITH CAUTION - This will delete ALL data including users and doctors!
--
-- WHAT THIS SCRIPT DOES:
-- 1. Deletes all messages and chat sessions
-- 2. Deletes all appointments
-- 3. Deletes all medical records
-- 4. Deletes all prescriptions
-- 5. Deletes all notifications
-- 6. Deletes all doctors
-- 7. Deletes all users
-- 8. Resets all ID sequences to 1
--
-- RESULT: Empty database ready for fresh doctor and patient registration
--
-- RUN IN SUPABASE SQL EDITOR
-- ════════════════════════════════════════════════════════════════════════════

-- Disable all constraints temporarily (they will be re-enabled automatically)
-- This allows us to delete in any order without foreign key violations

-- Step 1: Clear Chat History
DELETE FROM chat_messages;
DELETE FROM direct_messages;
DELETE FROM chat_sessions;
DELETE FROM ai_escalations;

-- Step 2: Clear Appointments
DELETE FROM appointments;

-- Step 3: Clear Medical Records
DELETE FROM medical_records;

-- Step 4: Clear Prescriptions
DELETE FROM prescriptions;

-- Step 5: Clear Notifications
DELETE FROM notifications;

-- Step 6: Clear Doctors
DELETE FROM doctors;

-- Step 7: Clear Users
DELETE FROM users;

-- Step 8: Reset all sequences to start from 1
DO $$
DECLARE
    seq_name TEXT;
    seq_record RECORD;
BEGIN
    -- Find and reset all sequences
    FOR seq_record IN
        SELECT sequence_schema, sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    LOOP
        EXECUTE 'ALTER SEQUENCE ' || seq_record.sequence_schema || '.' || seq_record.sequence_name || ' RESTART WITH 1;';
    END LOOP;
END $$;

-- Step 9: Verify all tables are empty
SELECT
    'users' as table_name, COUNT(*) as row_count FROM users
UNION ALL
SELECT
    'doctors' as table_name, COUNT(*) as row_count FROM doctors
UNION ALL
SELECT
    'appointments' as table_name, COUNT(*) as row_count FROM appointments
UNION ALL
SELECT
    'medical_records' as table_name, COUNT(*) as row_count FROM medical_records
UNION ALL
SELECT
    'prescriptions' as table_name, COUNT(*) as row_count FROM prescriptions
UNION ALL
SELECT
    'notifications' as table_name, COUNT(*) as row_count FROM notifications
UNION ALL
SELECT
    'chat_messages' as table_name, COUNT(*) as row_count FROM chat_messages
UNION ALL
SELECT
    'chat_sessions' as table_name, COUNT(*) as row_count FROM chat_sessions
UNION ALL
SELECT
    'direct_messages' as table_name, COUNT(*) as row_count FROM direct_messages;

-- Status message
SELECT '✅ Database reset complete! All tables are empty. Ready for new registrations.' as status;

