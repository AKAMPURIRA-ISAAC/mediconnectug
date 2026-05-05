# HealthBridge - Complete Deployment & Testing Guide

## 🚀 DEPLOYMENT CHECKLIST (Step-by-Step)

### Phase 1: Database Cleanup (5-10 minutes)

#### Step 1.1: Access Supabase Console
```
1. Go to https://supabase.com
2. Sign in to your project
3. Click "SQL Editor" in the left sidebar
4. Click "New Query" button (top right)
```

#### Step 1.2: Choose Your Database Cleanup Option

**OPTION A: Clean Doctors Only (Recommended)**
- Use if you want to keep existing patient data
- Safer for production
- Can run multiple times

```
1. Copy entire content from: supabase/clean_doctors.sql
2. Paste into SQL Editor query box
3. Click "Run" button (blue button, top right)
4. Wait for completion (should be instant)
5. Expected Result:
   ✓ 0 total_doctors
   ✓ Database cleaned successfully - Ready for new doctor registrations
```

**OPTION B: Complete Database Reset (Development Only)**
- Use if you want everything deleted
- Fresh start with empty database
- NOT recommended for production with data

```
1. Copy entire content from: supabase/reset_database.sql
2. Paste into SQL Editor query box
3. Click "Run" button
4. Wait for completion
5. Expected Result:
   ✓ All tables show 0 rows
   ✓ Database reset complete! All tables are empty.
```

#### Step 1.3: Verify Cleanup Success
```sql
-- Run this verification query in Supabase SQL Editor

SELECT 
    COUNT(*) as total_doctors,
    MAX(id) as highest_doctor_id
FROM doctors;

-- Expected: 0 total_doctors, NULL highest_doctor_id
```

---

### Phase 2: Android App Preparation (10-15 minutes)

#### Step 2.1: Clean Build Cache
```bash
# Open Terminal/Command Prompt in your project root
cd D:\HealthBridge\android_app

# Clean build cache
./gradlew cleanBuildCache

# Android Studio alternative:
# Go to: Build → Clean Project
```

#### Step 2.2: Full Rebuild
```bash
# Build the app in debug mode
./gradlew assembleDebug

# Or in Android Studio:
# Go to: Build → Build Bundle(s) / APK(s) → Build APK(s)
```

**Expected Output:**
```
BUILD SUCCESSFUL in XXs
```

#### Step 2.3: Locate APK
```
File location: app/build/outputs/apk/debug/app-debug.apk
```

---

### Phase 3: Installation & Testing (15-20 minutes)

#### Step 3.1: Install on Device/Emulator

**Option A: Using Android Studio**
```
1. Connect your Android device via USB (or launch emulator)
2. In Android Studio: Run → Run 'app'
3. Wait for app to install and launch
4. Expected: App opens showing Splash Screen
```

**Option B: Using Command Line**
```bash
# Uninstall previous version (optional)
adb uninstall com.healthbridge

# Install new version
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.healthbridge/com.healthbridge.SplashActivity
```

#### Step 3.2: Initial App Launch
```
1. App starts with Splash Screen
2. See Onboarding carousel
3. Tap "Skip" or complete onboarding
4. Should see Login screen with options:
   ✓ Sign In (existing user)
   ✓ Create Account (new patient)
   ✓ Register as Doctor (new doctor)
```

---

### Phase 4: Doctor Registration Flow Test (20-30 minutes)

#### Step 4.1: Test Patient Registration (Sanity Check)
```
1. Tap "Create Account" button
2. Fill in:
   - Full Name: Test Patient
   - Email: testpatient@test.com
   - Phone: +256700000000
   - Password: Password123
3. Tap "Sign Up"
4. Expected: Success message, redirected to Home page
5. Tap Menu → Settings → Logout
```

✅ **Verify**: Patient registration still works

#### Step 4.2: Test Doctor Registration - Successful Flow
```
1. On Login screen, tap "Register as Doctor"
2. Verify you see:
   ✅ Red banner: "Account registration is MANDATORY"
   ✅ "Select your hospital, pharmacy, or healthcare facility" message
3. Fill in:
   - Full Name: Dr. John Okello
   - Email: john.okello@med.ug
   - Phone: +256701234567
   - Specialty: SELECT → "General Practice"
   - Hospital/Pharmacy: SELECT → "Mulago National Referral Hospital - Kampala"
   - License Number: UMC/GP/2024/1001
   - Password: DoctorPass123
   - Confirm Password: DoctorPass123
4. Tap "Register as Doctor"
5. Expected: Success toast message, redirect to Doctor Home page
```

✅ **Verify**: Doctor successfully registered with pharmacy/hospital

#### Step 4.3: Test Hospital/Pharmacy Dropdown
```
1. Go back to Doctor Registration
2. Tap Hospital/Pharmacy dropdown
3. Scroll through options - should see:
   ✓ Mulago National Referral Hospital - Kampala
   ✓ Multiple regional hospitals with city names
   ✓ PHARMACIES: Kampala Pharmacy, Protector Pharmacy, etc.
   ✓ "Other - Please Specify" option
4. Select "Other - Please Specify"
5. Verify: Custom hospital text field appears
6. Type: "My Private Clinic - Entebbe"
7. Verify: Field accepts text and shows in validation
```

✅ **Verify**: Pharmacy list is complete and "Other" works

#### Step 4.4: Test Validation Messages
```
1. Skip filling any required field:
   - Missing name: "Enter your full name" error
   - Missing email: "Enter a valid email" error
   - Invalid specialty: Alert "Please select your specialty"
   - Missing hospital: Alert with "pharmacy" mentioned
   - Other selected without text: "Enter hospital, pharmacy, or facility name"
   - Missing license: "Enter your license number" error
   - Short password: "Password must be at least 6 characters" error
   - Mismatched passwords: "Passwords don't match" error
```

✅ **Verify**: All validation messages mention pharmacies where relevant

---

### Phase 5: Database Verification (10 minutes)

#### Step 5.1: Check New Doctor in Database
```sql
-- Run in Supabase SQL Editor

-- Check doctor was created
SELECT 
    id,
    name,
    email,
    specialty,
    hospital,
    license_number,
    created_at
FROM doctors
ORDER BY id DESC
LIMIT 1;

-- Expected: Shows your newly registered doctor
-- hospital should show: "Mulago National Referral Hospital - Kampala"
-- or your custom text if you used "Other"
```

#### Step 5.2: Check User Account Created
```sql
SELECT 
    id,
    name,
    email,
    user_type,
    created_at
FROM users
WHERE email = 'john.okello@med.ug'
ORDER BY id DESC
LIMIT 1;

-- Expected: user_type = 'doctor'
```

#### Step 5.3: Verify No Seeded Doctors Remain
```sql
SELECT COUNT(*) as doctor_count FROM doctors;

-- Expected: Should show at least 1 (your new doctor)
-- Should NOT show 10+ (which would be seeded doctors)
```

---

### Phase 6: Additional Functionality Tests (15-20 minutes)

#### Step 6.1: Test Doctor Login
```
1. Logout from current doctor account
2. On Login screen:
   - Email: john.okello@med.ug
   - Password: DoctorPass123
   - Select "Doctor" user type (if available)
3. Tap "Login"
4. Expected: Successfully logged in, see Doctor Home page
```

✅ **Verify**: Doctor login works with registered credentials

#### Step 6.2: Test Multiple Doctor Registrations
```
1. Logout
2. Register another doctor:
   - Name: Dr. Sarah Nakamya
   - Email: sarah.nakamya@med.ug
   - Specialty: Cardiology
   - Hospital: "City Pharmacy - Jinja" (pharmacy option)
   - License: UMC/CARD/2024/1002
3. Verify registration succeeds
4. Check database - should show 2 doctors
```

✅ **Verify**: Multiple doctors can register

#### Step 6.3: Test App Navigation
```
1. After doctor registration, verify:
   ✓ Can navigate to Doctor Home
   ✓ Can access appointments
   ✓ Can view profile
   ✓ Can logout without errors
```

✅ **Verify**: No crashes or errors in navigation

---

### Phase 7: Performance & Stability (10 minutes)

#### Step 7.1: Verify App Performance
```
1. Launch app cold start (force stop, relaunch)
2. Navigate through all screens:
   - Splash → Onboarding → Login
   - Register Patient → Home
   - Logout → Login
   - Register Doctor → Doctor Home
3. Check for:
   ✓ No crashes
   ✓ Smooth transitions
   ✓ UI elements render properly
   ✓ No lag or freezing
```

✅ **Verify**: App is stable and responsive

#### Step 7.2: Test Network Error Handling
```
1. Turn off WiFi and mobile data (Airplane Mode)
2. Try to register doctor
3. Expected: Network error message
4. Turn WiFi back on
5. Try again - should succeed
```

✅ **Verify**: Proper error handling

---

### Phase 8: Final Production Checklist

#### Before Deployment - Run This Checklist:

```
DATABASE:
☑ Seeded doctors removed (clean_doctors.sql run)
☑ Database shows 0 doctors initially
☑ New doctors register successfully
☑ Doctor data is correct in database
☑ No duplicate entries

CODE:
☑ DoctorRegisterActivity compiles without errors
☑ HealthcareConstants includes 60+ facilities
☑ Pharmacies listed with Uganda cities
☑ "Other" option works for custom entries

UI:
☑ Red banner shows "Account registration is MANDATORY"
☑ Hospital/pharmacy dropdown shows all options
☑ Custom hospital field appears for "Other"
☑ All validation messages mention pharmacies

FUNCTIONALITY:
☑ Patient registration works
☑ Doctor registration works
☑ Doctor login works
☑ All fields validate correctly
☑ No crashes during registration
☑ Database updates correctly

TESTING:
☑ 5+ successful doctor registrations tested
☑ Pharmacy options selected and saved
☑ Custom hospital names accepted
☑ Validation messages display correctly
☑ App runs on Android 8.0+
```

---

## 📱 Build Production APK (for Play Store)

### Step 1: Build Release APK
```bash
# In project root directory
./gradlew assembleRelease

# Expected output:
# app/build/outputs/apk/release/app-release.apk
```

### Step 2: Sign Release APK (if needed)
```bash
# Windows:
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore mykey.keystore \
  app/build/outputs/apk/release/app-release.apk \
  alias_name

# Have keystore password ready
```

### Step 3: Upload to Play Store
```
1. Go to Google Play Console
2. Select your app
3. Go to Release → Production
4. Click "Create Release"
5. Upload signed APK
6. Add release notes mentioning:
   - Strict doctor account registration
   - Support for 60+ healthcare facilities
   - Pharmacy selection support
7. Review and publish
```

---

## 🔍 Troubleshooting During Deployment

### Issue: "BuildConfig not found"
**Solution:**
```bash
./gradlew cleanBuildCache
./gradlew assembleDebug
```

### Issue: "App crashes on launch"
**Solution:**
1. Check logcat: `adb logcat | grep -i error`
2. Verify all imports are correct
3. Rebuild: `./gradlew cleanBuildCache assembleDebug`

### Issue: "Spinner shows no hospitals"
**Solution:**
1. Verify HealthcareConstants.kt is updated
2. Rebuild and reinstall app completely
3. Clear app cache: Settings → Apps → HealthBridge → Storage → Clear Cache

### Issue: "Doctor registration fails"
**Solution:**
1. Check backend is running
2. Verify email is unique (not used before)
3. Check phone number format
4. Verify all required fields filled

### Issue: "Can't see pharmacies in list"
**Solution:**
1. Scroll down in dropdown (list is long)
2. Clear app cache and restart
3. Verify HealthcareConstants.kt includes pharmacies
4. Check database directly for facility names

### Issue: "Database script fails to run"
**Solution:**
1. Ensure you're in Supabase SQL Editor (not local)
2. Select correct database/project
3. Verify entire script pasted (not partial)
4. Try running in small chunks if connection times out

---

## ✅ Pre-Launch Verification

**Run this final checklist before going live:**

```bash
# 1. Clean build everything
./gradlew cleanBuildCache clean

# 2. Rebuild
./gradlew assembleDebug

# 3. Run instrumented tests (if available)
./gradlew connectedAndroidTest

# 4. Check APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk
# Expected: < 100MB

# 5. Install fresh
adb uninstall com.healthbridge
adb install app/build/outputs/apk/debug/app-debug.apk

# 6. Run as new user
adb shell am start com.healthbridge/com.healthbridge.SplashActivity
```

---

## 📊 Deployment Progress Tracker

```
Phase 1: Database Cleanup
[ ] Access Supabase
[ ] Run clean_doctors.sql
[ ] Verify script succeeded
[ ] Confirm 0 doctors in DB

Phase 2: App Build
[ ] Clean build cache
[ ] Full rebuild successful
[ ] APK generated (< 100MB)

Phase 3: Installation
[ ] APK installed on device
[ ] App launches without crash
[ ] Splash screen displays

Phase 4: Doctor Registration Test
[ ] Patient registration works
[ ] Doctor registration page loads
[ ] Hospital dropdown populated
[ ] Pharmacy options visible
[ ] "Other" custom entry works
[ ] Complete registration successful

Phase 5: Database Verification
[ ] New doctor appears in database
[ ] Hospital/pharmacy saved correctly
[ ] User account created
[ ] No seeded doctors remain

Phase 6: Extended Testing
[ ] Doctor login works
[ ] Multiple registrations work
[ ] Navigation stable
[ ] No crashes
[ ] Performance acceptable

Phase 7: Production Ready
[ ] All tests passed
[ ] Screenshots captured
[ ] Release notes prepared
[ ] Ready for deployment
```

---

## 🚀 Final Deployment Steps

```
1. ✅ Database cleaned
2. ✅ App tested thoroughly  
3. ✅ All checklist items verified
4. ✅ Performance acceptable
5. ✅ Build release APK
6. ✅ Sign APK
7. ✅ Upload to Play Store OR
8. ✅ Distribute to users

LAUNCH! 🎉
```

---

## 📞 Quick Reference Commands

```bash
# Clean everything
./gradlew cleanBuildCache clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Install to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat -s "HealthBridge"

# Uninstall
adb uninstall com.healthbridge

# Clear app data
adb shell pm clear com.healthbridge

# Launch app
adb shell am start -n com.healthbridge/.SplashActivity
```

---

## 📝 Deployment Status

- **Database Scripts**: ✅ Ready
- **Android Code**: ✅ Ready  
- **Documentation**: ✅ Complete
- **Testing Guide**: ✅ Provided
- **Deployment Status**: ✅ READY TO LAUNCH

**Next Step**: Follow Phase 1 above to start deployment!

---

*Last Updated: May 5, 2026*  
*Version: 2.1.4*  
*Status: ✅ Ready for Production*

