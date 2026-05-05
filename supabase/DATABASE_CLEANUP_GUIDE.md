# MediConnectUG - Doctor Account Registration & Database Reset Guide

## 🎯 Overview

This guide explains how to enforce strict account registration for doctors and how to manage the database to ensure only newly registered doctors exist in the system.

## 📋 Key Changes Made

### 1. **Strict Account Registration for Doctors**
- **Requirement**: All doctors MUST create an account through the registration process
- **No Seeded Data**: The database no longer contains pre-seeded doctor accounts
- **Location-Aware**: Doctors select from Uganda-based hospitals and pharmacies
- **Pharmacy Options**: Added 14+ pharmacies across major Uganda cities (Kampala, Jinja, Masaka, Mbarara, Fort Portal, Gulu, Arua, Soroti, Mbale)

### 2. **Database Cleaning Scripts**

We provide two SQL scripts for database management:

#### **Option 1: `clean_doctors.sql` - Remove Only Seeded Doctors**
- **Use When**: You want to remove only doctor records but keep patient and appointment data
- **Effect**: Deletes all doctors and resets doctor ID sequences
- **Safe**: Does not affect user or appointment data if you want to preserve it
- **Location**: `supabase/clean_doctors.sql`

```sql
-- Run in Supabase SQL Editor
-- Deletes all doctors and resets sequences
```

#### **Option 2: `reset_database.sql` - Full Database Reset**
- **Use When**: You want a completely clean start with NO historical data
- **Effect**: Deletes everything:
  - All users and patients
  - All doctors
  - All appointments
  - All medical records
  - All prescriptions
  - All notifications
  - All chat messages and sessions
- **Resets**: All ID sequences back to 1
- **Location**: `supabase/reset_database.sql`

```sql
-- WARNING: Only run this if you want to delete EVERYTHING
-- Use in Supabase SQL Editor
```

### 3. **HealthcareConstants Updated**

The Android app now uses an enhanced hospital/pharmacy list with:

**New Features:**
- ✅ 60+ facilities including hospitals, pharmacies, and clinics
- ✅ All locations are in Uganda (city specified)
- ✅ Includes major pharmacies in key cities:
  - Kampala Pharmacy - Kampala City Centre
  - Protector Pharmacy - Old Kampala
  - Alliance Pharmacy - Ntinda
  - City Pharmacy - Jinja
  - Central Pharmacy - Mbarara
  - Unity Pharmacy - Fort Portal
  - Hope Pharmacy - Gulu
  - And more...
- ✅ Organized by facility type:
  - National Referral Hospitals
  - Regional Referral Hospitals
  - Private Hospitals
  - Pharmacies (NEW)
  - Clinics and Diagnostic Centers
- ✅ "Other" option to specify custom location

**Doctor Registration Flow:**
```
1. Doctor opens registration screen
2. Fills: Name, Email, Phone, Specialty
3. MUST select Hospital/Pharmacy OR choose "Other" and specify
4. If "Other", must enter hospital/pharmacy name and location
5. Fills: License Number, Password
6. Submits registration
7. Doctor account created in database
```

## 🗄️ How to Use the Database Scripts

### Step 1: Access Supabase

1. Go to [supabase.com](https://supabase.com)
2. Sign in to your project
3. Click **SQL Editor** in the left sidebar
4. Click **"New Query"**

### Step 2: Choose Your Script

#### For Production (Keeping Patient Data):
```
Use: clean_doctors.sql
This removes only seeded doctors but preserves all patient and appointment data
```

#### For Development/Fresh Start:
```
Use: reset_database.sql
This cleans everything and resets all sequences
```

### Step 3: Run the Script

1. Copy the entire SQL script
2. Paste into Supabase SQL Editor
3. Click **"Run"** button
4. Wait for confirmation message

### Expected Output:

**For clean_doctors.sql:**
```
✓ 0 total_doctors
✓ Database cleaned successfully - Ready for new doctor registrations
```

**For reset_database.sql:**
```
✓ users: 0
✓ doctors: 0
✓ appointments: 0
✓ medical_records: 0
✓ prescriptions: 0
✓ notifications: 0
✓ chat_messages: 0
✓ chat_sessions: 0
✓ direct_messages: 0
✓ Database reset complete! All tables are empty. Ready for new registrations.
```

## 📱 Android App Changes

### DoctorRegisterActivity.kt
- ✅ Removed duplicate code
- ✅ Uses HealthcareConstants.HOSPITALS
- ✅ Hospital spinner shows "Other - Please Specify" option
- ✅ Custom field appears when "Other" is selected
- ✅ Enhanced validation messages mentioning pharmacies

### HealthcareConstants.kt
- ✅ Added 14+ pharmacies across Uganda
- ✅ All locations now include city names (e.g., "Kampala", "Jinja", "Mbarara")
- ✅ Better categorization: `getHospitalCategory()` method
- ✅ New method: `getLocationFromHospital()` to extract location
- ✅ Comprehensive documentation about mandatory registration

### Database Schema Files
- ✅ `schema.sql`: Removed sample doctor INSERT statements
- ✅ `doctor_chat_schema.sql`: Removed sample doctor UPDATE statements
- ✅ Database now starts empty - only accepts new registrations

## 🔒 Security & Best Practices

1. **No More Seeded Accounts**
   - No default doctor credentials
   - All doctors must verify their account during registration
   - Passwords are hashed with bcryptjs (10 rounds)

2. **Hospital/Pharmacy Validation**
   - Doctors select from pre-defined list OR specify custom location
   - Location specified prevents fraudulent facility names
   - Facility category automatically determined

3. **Location Awareness**
   - All locations are Uganda-based
   - Helps with regulatory compliance and local healthcare system integration
   - Easier to audit and verify doctor credentials

## ✅ Verification Checklist

After implementing these changes:

- [ ] Run `clean_doctors.sql` or `reset_database.sql` in Supabase
- [ ] Verify database is empty by checking:
  ```sql
  SELECT COUNT(*) FROM doctors;  -- Should return 0
  SELECT COUNT(*) FROM users;    -- Should return 0
  ```
- [ ] Build and install updated Android app:
  ```bash
  ./gradlew cleanBuildCache
  ./gradlew assembleDebug
  ```
- [ ] Test doctor registration:
  - Open app → Register as Doctor
  - Fill all fields
  - Select a hospital/pharmacy from dropdown
  - Or select "Other" and type custom location
  - Submit registration
  - Verify new doctor appears in database

- [ ] Test patient registration still works:
  - Open app → Create Account (Patient)
  - Basic registration flow
  - Verify patient account created

## 🚀 Next Steps

1. **Run Database Cleanup**
   - Choose appropriate script (clean_doctors.sql or reset_database.sql)
   - Execute in Supabase SQL Editor
   - Verify successful completion

2. **Deploy Updated App**
   - Build Android app with new changes
   - Test doctor registration flow
   - Test patient functionality

3. **Monitor New Registrations**
   - All new doctors will have registered accounts
   - No more seeded/sample doctors
   - Each doctor has verifiable credentials

## 📞 Support

For questions or issues:
1. Check that you're using the correct SQL script
2. Verify all changes are deployed to the app
3. Test in Android Emulator before production
4. Check Supabase logs for any SQL errors

## 📝 Important Notes

⚠️ **CAUTION with reset_database.sql**
- This script deletes ALL data - use only for development/testing
- Not recommended for production with existing data
- Always backup production data before running

✅ **Safe Option: clean_doctors.sql**
- Only removes doctors
- Preserves patient data and appointments
- Can be run safely multiple times
- Recommended for production

---

**Last Updated**: May 5, 2026
**Version**: 2.1.4
**Status**: ✅ Ready for Deployment

