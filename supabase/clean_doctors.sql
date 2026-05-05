-- ════════════════════════════════════════════════════════════════════════════
-- MediConnectUG Database Clean-Up: Remove All Seeded Doctors
-- ════════════════════════════════════════════════════════════════════════════
--
-- PURPOSE:
-- Removes all sample/seeded doctor accounts from the database.
-- Ensures database starts EMPTY with only new doctor accounts created
-- through proper user registration process.
--
-- IMPORTANT: After running this script:
-- 1. Run this script ONCE
-- 2. All seeded doctors will be deleted
-- 3. Only doctors who register through the app will exist
-- 4. New doctor IDs will start from 1
--
-- RUN IN SUPABASE SQL EDITOR
-- ════════════════════════════════════════════════════════════════════════════

-- Step 1: Delete all records from the doctors table
-- This removes all pre-seeded/sample doctors
DELETE FROM doctors;

-- Step 2: Reset the sequence for the doctors table primary key
-- Ensures that the first new doctor registered gets ID = 1
DO $$
DECLARE
    seq_name TEXT;
BEGIN
    -- Find the sequence associated with the doctors table primary key
    SELECT pg_get_serial_sequence('doctors', 'id') INTO seq_name;

    -- Reset the sequence to start from 1
    IF seq_name IS NOT NULL THEN
        EXECUTE 'ALTER SEQUENCE ' || seq_name || ' RESTART WITH 1;';
    END IF;
END $$;

-- Step 3: Verify the table is empty
-- This confirms no doctors exist in the database
SELECT
    COUNT(*) as total_doctors,
    'Database cleaned successfully - Ready for new doctor registrations' as status
FROM doctors;
