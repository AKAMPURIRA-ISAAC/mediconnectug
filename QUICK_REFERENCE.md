# HealthBridge - Quick Reference Card

## 📱 QUICK BUILD & DEPLOY

### One-Command Build & Install
```powershell
# Windows PowerShell (navigate to project root first)
cd D:\HealthBridge\android_app
.\scripts\deploy-app.ps1
```

This automatically:
- ✅ Cleans build cache
- ✅ Builds debug APK
- ✅ Uninstalls old version
- ✅ Installs new version
- ✅ Launches app

---

## 🗄️ DATABASE OPERATIONS

### Before Every Deployment: Clean Database

**In Supabase SQL Editor:**

```sql
-- Step 1: Run this to remove all seeded doctors
DELETE FROM doctors;

-- Step 2: Reset ID sequence
DO $$
DECLARE
    seq_name TEXT;
BEGIN
    SELECT pg_get_serial_sequence('doctors', 'id') INTO seq_name;
    IF seq_name IS NOT NULL THEN
        EXECUTE 'ALTER SEQUENCE ' || seq_name || ' RESTART WITH 1;';
    END IF;
END $$;

-- Step 3: Verify (should show 0)
SELECT COUNT(*) as doctor_count FROM doctors;
```

Or use prewritten script:
```
Copy content from: supabase/clean_doctors.sql
Paste in Supabase SQL Editor
Click "Run"
```

---

## ✅ TESTING CHECKLIST (5 mins per item)

### 1. Patient Registration ✓
```
1. App Home → Create Account
2. Fill: Name, Email, Phone, Password
3. Tap Sign Up
4. Expected: Success, redirect to Home
```

### 2. Doctor Registration ✓
```
1. App Home → Register as Doctor
2. Verify red banner: "Account registration is MANDATORY"
3. Fill all fields
4. Hospital: Choose "Mulago National Referral Hospital - Kampala"
5. License: Any number (e.g., UMC/2024/001)
6. Password: 6+ chars
7. Tap Register
8. Expected: Success, redirect to Doctor Home
```

### 3. Pharmacy Selection ✓
```
1. Doctor Registration → Hospital Dropdown
2. Scroll and select: "Kampala Pharmacy - Kampala City Centre"
3. Complete registration
4. Expected: Works without errors
```

### 4. Other Option ✓
```
1. Doctor Registration → Hospital Dropdown
2. Select: "Other - Please Specify"
3. Text field appears
4. Type: "My Clinic - Kampala"
5. Complete registration
6. Expected: Custom text saves
```

### 5. Database Verification ✓
```
In Supabase SQL Editor:
SELECT * FROM doctors WHERE email = 'YOUR_TEST_EMAIL';

Expected: Shows your registered doctor with correct:
- name
- email
- specialty
- hospital (or custom text)
- license_number
```

---

## 🔍 TROUBLESHOOTING

| Issue | Solution |
|-------|----------|
| App won't build | `./gradlew cleanBuildCache` then rebuild |
| App crashes on startup | Check `adb logcat` for errors; rebuild |
| Can't see pharmacies | Rebuild app, clear cache, restart |
| Doctor registration fails | Verify backend running, email unique |
| Database script fails | Copy ENTIRE script, paste in Supabase |
| No device connected | `adb devices` - connect phone or start emulator |

---

## 📊 VERIFICATION COMMANDS

```powershell
# Check if app is installed
adb shell pm list packages | findstr healthbridge

# View app logs
adb logcat -s "HealthBridge"

# Get app info
adb shell dumpsys package com.healthbridge

# Clear app cache
adb shell pm clear com.healthbridge

# Restart app
adb shell am start -n com.healthbridge/.SplashActivity
```

---

## 🚀 DEPLOYMENT FLOW

```
1. Database
   ├─ Run clean_doctors.sql in Supabase
   └─ Verify: SELECT COUNT(*) FROM doctors; → 0

2. Build
   ├─ ./gradlew cleanBuildCache
   └─ ./gradlew assembleDebug

3. Install
   ├─ adb install -r app/build/outputs/apk/debug/app-debug.apk
   └─ Or use: .\scripts\deploy-app.ps1

4. Test
   ├─ Patient registration
   ├─ Doctor registration
   ├─ Pharmacy selection
   └─ Database check

5. Production
   ├─ ./gradlew assembleRelease
   ├─ Sign APK
   └─ Upload to Play Store
```

---

## 📝 FILE LOCATIONS

```
Project Root: D:\HealthBridge\android_app

Key Files:
├─ app/src/main/java/com/healthbridge/
│  ├─ DoctorRegisterActivity.kt (registration logic)
│  └─ util/HealthcareConstants.kt (hospital/pharmacy list)
├─ app/src/main/res/layout/
│  └─ activity_doctor_register.xml (UI)
├─ supabase/
│  ├─ clean_doctors.sql (cleanup script)
│  ├─ reset_database.sql (full reset)
│  └─ schema.sql (database schema)
├─ scripts/
│  └─ deploy-app.ps1 (one-click deploy)
└─ Documentation/
   ├─ DEPLOYMENT_GUIDE.md (detailed steps)
   └─ IMPLEMENTATION_COMPLETE_v2.1.4.md (overview)
```

---

## ⏱️ TIMING ESTIMATES

| Task | Time |
|------|------|
| Clean database | 1-2 min |
| Build APK | 3-5 min |
| Install app | 1-2 min |
| Test flows | 10-15 min |
| Verify database | 2-3 min |
| **Total** | **20-30 min** |

---

## 🎯 SUCCESS INDICATORS

✅ **System is ready when:**
- [x] App builds without errors
- [x] App installs on device
- [x] Doctor registration completes
- [x] Pharmacies visible in dropdown
- [x] New doctor appears in database
- [x] No crashes during testing

---

## 📞 QUICK LINKS

| Resource | Location |
|----------|----------|
| Full Deployment Guide | DEPLOYMENT_GUIDE.md |
| Implementation Summary | IMPLEMENTATION_COMPLETE_v2.1.4.md |
| Database Cleanup Guide | supabase/DATABASE_CLEANUP_GUIDE.md |
| Hospital/Pharmacy List | app/src/main/java/.../HealthcareConstants.kt |

---

## 🚀 ONE-LINE COMMANDS

```bash
# Build and install in one command
./gradlew cleanBuildCache assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# View registration success/failures
adb logcat -s "HealthBridge" | findstr -i "register\|error\|success"

# Check all doctors in database
echo "SELECT name, email, hospital, created_at FROM doctors ORDER BY created_at DESC;" | supabase-cli query

# Full reset and redeploy
./gradlew cleanBuildCache clean && .\scripts\deploy-app.ps1
```

---

## 📋 DAILY CHECKLIST

**Before committing code:**
- [ ] App builds without warnings
- [ ] No crashes on startup
- [ ] Doctor registration works
- [ ] Database shows registered doctor
- [ ] Pharmacy options visible
- [ ] Can login as doctor

**Before releasing to production:**
- [ ] All manual tests passed
- [ ] Logcat shows no errors
- [ ] Database is clean (optional)
- [ ] Release APK built and signed
- [ ] Version numbers updated
- [ ] Release notes prepared

---

**Version**: 2.1.4  
**Last Updated**: May 5, 2026  
**Status**: ✅ Ready to Deploy

