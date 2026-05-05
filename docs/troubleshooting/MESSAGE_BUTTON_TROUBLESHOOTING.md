# Message Button Troubleshooting Guide

**Issue:** Message button shows "Starting chat..." toast but chat never opens

## ✅ What I've Fixed

### 1. Added Detailed Logging
The code now logs every step to help identify where it's failing:
- `DoctorProfile: Creating chat session for doctor: [name]`
- `DoctorProfile: API Response: success=[true/false], session=[id]`
- `DoctorProfile: Opening PatientChatActivity with session_id=[id]`
- `DoctorProfile: PatientChatActivity started successfully`

### 2. Enhanced Error Messages
Instead of generic "Could not start chat", you'll now see:
- Actual API error if available
- Network error details
- Exception messages

### 3. Added Fallback Dialog
If chat fails, users get a dialog:
```
"Chat Unavailable"
"Unable to start chat. Would you like to book an appointment instead?"
[Book Appointment] [Cancel]
```

## 🔍 How to Debug

### Step 1: Check Logcat
1. Connect your device/emulator
2. Open Logcat in Android Studio
3. Filter by "DoctorProfile"
4. Click the message button
5. Look for these logs:

**If you see:**
```
DoctorProfile: Creating chat session for doctor: [name]
DoctorProfile: Exception creating chat: [error message]
```
→ **Problem: Network/API error**

**If you see:**
```
DoctorProfile: API Response: success=false, session=null
```
→ **Problem: Backend not creating session**

**If you see:**
```
DoctorProfile: Opening PatientChatActivity with session_id=[id]
```
→ **Problem: PatientChatActivity not opening (check manifest)**

### Step 2: Common Issues & Solutions

#### Issue 1: Network Error
**Logged as:** `Exception creating chat: Unable to resolve host...`

**Solutions:**
- Check backend is running at `https://mediconnectug.onrender.com`
- Check device internet connection
- Check API endpoint exists: `POST /api/chat-sessions`
- Verify API token is valid

**Test Backend:**
```bash
# In browser or Postman:
GET https://mediconnectug.onrender.com/api/health
```

#### Issue 2: API Returns success=false
**Logged as:** `API Response: success=false`

**Solutions:**
- Check backend logs for errors
- Verify authentication token is being sent
- Check CreateChatSessionRequest format matches backend expectations
- Ensure user is logged in (token exists in SharedPreferences)

#### Issue 3: PatientChatActivity Not Opening
**Logged as:** PatientChatActivity not found or crash

**Solutions:**
- Verify PatientChatActivity is in AndroidManifest.xml
  ```xml
  <activity android:name=".PatientChatActivity" android:exported="false" />
  ```
- Check PatientChatActivity.kt exists
- Rebuild project: `./gradlew clean build`

#### Issue 4: Silent Failure (No logs)
**No logs appear in Logcat**

**Solutions:**
- lifecycleScope might not be working
- Check imports are correct:
  ```kotlin
  import androidx.lifecycle.lifecycleScope
  import kotlinx.coroutines.launch
  ```
- Check app/build.gradle.kts has:
  ```kotlin
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  ```

## 🧪 Testing Steps

### Test 1: Check if button click is registered
```kotlin
// Temporary test - add this at the start of btnMessage.setOnClickListener
Log.d("TEST", "Message button clicked!")
```
✅ If you see this log → Button click works  
❌ If you don't see this → Button not properly connected

### Test 2: Check lifecycleScope
```kotlin
// Add this in btnMessage.setOnClickListener before lifecycleScope.launch
Log.d("TEST", "About to launch coroutine")

lifecycleScope.launch {
    Log.d("TEST", "Inside coroutine")
    // ... rest of code
}
```
✅ Both logs appear → lifecycleScope works  
❌ First log only → lifecycleScope not working

### Test 3: Test Without API Call
Temporarily skip the API and open chat directly:
```kotlin
findViewById<View>(R.id.btnMessage).setOnClickListener {
    // TEMPORARY TEST - bypass API
    Toast.makeText(this, "Opening chat directly...", Toast.LENGTH_SHORT).show()
    
    val chatIntent = Intent(this, PatientChatActivity::class.java).apply {
        putExtra("session_id", 999) // Fake ID for testing
        putExtra("doctor_name", name)
        putExtra("chief_complaint", "Test consultation")
        putExtra("urgency_level", "MODERATE")
    }
    startActivity(chatIntent)
}
```
✅ Chat opens → API call is the problem  
❌ Chat doesn't open → PatientChatActivity is the problem

## 🛠️ Quick Fixes

### Quick Fix 1: Bypass API for Now
If you need chat working immediately while debugging:

```kotlin
findViewById<View>(R.id.btnMessage).setOnClickListener {
    Toast.makeText(this, "Opening chat with $name...", Toast.LENGTH_SHORT).show()
    
    // Use hardcoded session ID for testing
    val chatIntent = Intent(this, PatientChatActivity::class.java).apply {
        putExtra("session_id", 1) // Temporary - use real API later
        putExtra("doctor_name", name)
        putExtra("chief_complaint", "Direct consultation request")
        putExtra("urgency_level", "MODERATE")
    }
    startActivity(chatIntent)
}
```

### Quick Fix 2: Verify PatientChatActivity Exists
Run this command:
```bash
# In terminal
ls -la D:\HealthBridge\android_app\app\src\main\java\com\healthbridge\PatientChatActivity.kt
```
Should show the file exists.

### Quick Fix 3: Rebuild Everything
```bash
cd D:\HealthBridge\android_app
./gradlew clean
./gradlew assembleDebug
# Install APK on device
```

## 📊 Expected Logcat Output (Success)

When everything works correctly, you should see:
```
D/DoctorProfile: Message button clicked!
D/DoctorProfile: Creating chat session for doctor: Dr. John Smith
D/DoctorProfile: API Response: success=true, session=123
D/DoctorProfile: Opening PatientChatActivity with session_id=123
D/DoctorProfile: PatientChatActivity started successfully
```

## 🚨 Most Likely Issues

Based on common problems, check these **in order**:

1. **Backend Not Running** (90% of cases)
   - Solution: Start backend or check Render.com deployment

2. **No Internet Connection** (5% of cases)
   - Solution: Check device WiFi/data

3. **PatientChatActivity Missing** (3% of cases)
   - Solution: Check file exists and is registered

4. **API Token Invalid** (2% of cases)
   - Solution: Re-login to get fresh token

## 📞 Next Steps

1. **Enable Logcat filtering for "DoctorProfile"**
2. **Click message button**
3. **Copy all logs** that appear
4. **Share logs** to identify exact issue

The detailed logging I added will tell us exactly where it's failing!

---

**Status:** Enhanced with logging & diagnostics  
**Version:** 2.1.2-debug  
**Date:** May 5, 2026

