# 🗄️ Database Migration - Doctor Schema Update

## ⚠️ IMPORTANT: Run This Before Deployment

The `doctors` table is missing critical fields that the application expects. You MUST run this migration before doctors can register or use the app.

---

## 🔧 Migration Script

### Option 1: If Table Doesn't Exist Yet (Fresh Setup)
Run the entire `supabase/schema.sql` file in Supabase SQL Editor.

### Option 2: If Table Already Exists (Current Setup)

**Copy and paste this into Supabase SQL Editor:**

```sql
-- Add missing columns to doctors table
ALTER TABLE doctors 
ADD COLUMN IF NOT EXISTS user_id INT REFERENCES users(id) ON DELETE CASCADE,
ADD COLUMN IF NOT EXISTS hospital VARCHAR(200),
ADD COLUMN IF NOT EXISTS license_number VARCHAR(100);

-- Verify the changes
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'doctors' 
ORDER BY ordinal_position;
```

---

## 📋 What Gets Added

| Column | Type | Purpose |
|--------|------|---------|
| `user_id` | INT (Foreign Key) | Links doctor to their user account |
| `hospital` | VARCHAR(200) | Hospital/clinic name from registration |
| `license_number` | VARCHAR(100) | Medical license number |

---

## ✅ Verification Steps

After running the migration:

```sql
-- Check that columns were added
SELECT * FROM doctors LIMIT 1;

-- Should return columns for:
-- id, user_id, name, specialty, rating, review_count, 
-- consultation_fee, experience_years, hospital, 
-- license_number, is_online, created_at
```

---

## 🐛 Troubleshooting

### Error: "Column already exists"
This is fine! The `IF NOT EXISTS` clause handles this. The migration will skip existing columns.

### Error: "Relation 'doctors' does not exist"
This means the table hasn't been created yet. Run the full `schema.sql` file instead.

### Doctor Registration Still Fails
1. Verify migration ran successfully
2. Restart backend server
3. Clear browser cache
4. Re-register doctor

---

## 📝 SQL Syntax Explanation

```sql
-- ALTER TABLE: Modify existing table
ALTER TABLE doctors 

-- ADD COLUMN IF NOT EXISTS: Only add if it doesn't exist
ADD COLUMN IF NOT EXISTS user_id INT 

-- REFERENCES users(id) ON DELETE CASCADE: 
-- Foreign key that deletes doctor record if user is deleted
REFERENCES users(id) ON DELETE CASCADE,

-- Add text field for hospital name (200 chars max)
ADD COLUMN IF NOT EXISTS hospital VARCHAR(200),

-- Add text field for license number (100 chars max)
ADD COLUMN IF NOT EXISTS license_number VARCHAR(100);
```

---

## 🔐 Security Notes

- `user_id` foreign key ensures doctors are linked to valid users
- Doctor records auto-delete if their user account is deleted
- License number is stored as plain text (consider encryption in future)
- Hospital name is non-sensitive

---

## 📊 Current Schema (After Migration)

```sql
CREATE TABLE doctors (
  id                 SERIAL PRIMARY KEY,
  user_id            INT REFERENCES users(id) ON DELETE CASCADE,     -- ✅ NEW
  name               VARCHAR(100) NOT NULL,
  specialty          VARCHAR(100) NOT NULL,
  rating             NUMERIC(3,1) DEFAULT 4.5,
  review_count       INT DEFAULT 0,
  consultation_fee   INT DEFAULT 50000,
  experience_years   INT DEFAULT 5,
  hospital           VARCHAR(200),                                    -- ✅ NEW
  license_number     VARCHAR(100),                                    -- ✅ NEW
  is_online          BOOLEAN DEFAULT FALSE,
  created_at         TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 🚀 Deployment Timeline

1. **Before Anything:** Run migration above
2. **Then:** Deploy backend changes (server.js)
3. **Then:** Rebuild Android app
4. **Test:** Doctor registration, login, appointments, chats

---

## 🆘 Still Having Issues?

1. Check Supabase logs for migration errors
2. Verify database connection is active
3. Try running migration multiple times (idempotent)
4. Check doctor registration debug logs in backend
5. Restart backend after migration

---

**⚡ Remember:** Migration must complete BEFORE deploying new code!

**Status:** Ready to run immediately

