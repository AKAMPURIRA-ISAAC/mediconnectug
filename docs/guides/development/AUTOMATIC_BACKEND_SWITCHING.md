# 🔧 Automatic Backend Switching - Setup Complete!

## ✅ What Was Implemented

Your app now **automatically switches between local and production backends** with a simple flag!

---

## 🎯 How It Works

### Configuration File
**Location:** `app/src/main/java/com/healthbridge/network/ApiService.kt`

```kotlin
private const val USE_LOCAL_BACKEND = true  // ← One-line switch!

private const val LOCAL_URL = "http://10.0.2.2:3001/"              
private const val PRODUCTION_URL = "https://mediconnectug.onrender.com/"

private val BASE_URL = if (USE_LOCAL_BACKEND) LOCAL_URL else PRODUCTION_URL
```

---

## 🚀 Usage

### For Development (Local Backend)
```kotlin
private const val USE_LOCAL_BACKEND = true  // ✅ Use local backend
```

**What this does:**
- App connects to: `http://10.0.2.2:3001/` (emulator localhost)
- Perfect for testing changes locally
- Requires local backend running (`npm start` in D:\HealthBridge\backend)

### For Production (Render Backend)
```kotlin
private const val USE_LOCAL_BACKEND = false  // ✅ Use production
```

**What this does:**
- App connects to: `https://mediconnectug.onrender.com/`
- Uses production database
- No local backend needed

---

## 📱 Testing Scenarios

### Scenario 1: Development on Emulator
```kotlin
USE_LOCAL_BACKEND = true
LOCAL_URL = "http://10.0.2.2:3001/"  // Already configured ✅
```

**Steps:**
1. Start local backend: `cd D:\HealthBridge\backend && npm start`
2. Build app: `gradlew assembleDebug`
3. Install APK on emulator
4. App automatically uses local backend!

### Scenario 2: Development on Physical Device
```kotlin
USE_LOCAL_BACKEND = true
LOCAL_URL = "http://192.168.1.100:3001/"  // ← Your PC IP
```

**Steps:**
1. Find your PC IP: `ipconfig` → Look for IPv4 Address
2. Update LOCAL_URL with your IP
3. Start local backend
4. Build and install app
5. Make sure phone and PC are on same WiFi

### Scenario 3: Production Testing
```kotlin
USE_LOCAL_BACKEND = false  // Switch to production
```

**Steps:**
1. Change flag to `false`
2. Build app: `gradlew assembleDebug`
3. Install APK
4. App uses production Render backend

### Scenario 4: Production Release
```kotlin
USE_LOCAL_BACKEND = false  // Must be false for release!
```

**Steps:**
1. Ensure flag is `false`
2. Build release: `gradlew assembleRelease`
3. Upload to Play Store
4. Users get production backend

---

## 🔍 Debugging

### Check Which Backend Is Active

The app logs the backend configuration on startup:

```
I/ApiClient: ============================================================
I/ApiClient: 🔧 Backend Configuration:
I/ApiClient:    Mode: LOCAL DEVELOPMENT
I/ApiClient:    Backend URL: http://10.0.2.2:3001/
I/ApiClient:    Auto-Switching: ENABLED ✅
I/ApiClient: ============================================================
```

**View logs:**
- Android Studio → Logcat → Filter: "ApiClient"
- Or via command: `adb logcat -s ApiClient:*`

---

## 📊 Quick Reference

| Situation | Setting | Backend URL | Requires |
|-----------|---------|-------------|----------|
| Local Dev (Emulator) | `true` | `http://10.0.2.2:3001/` | Local backend running |
| Local Dev (Phone) | `true` | `http://YOUR_IP:3001/` | Local backend + same WiFi |
| Production Test | `false` | `https://mediconnectug.onrender.com/` | Internet connection |
| Release Build | `false` | `https://mediconnectug.onrender.com/` | Internet connection |

---

## 🎨 Benefits of This Approach

### ✅ Advantages
1. **One-Line Switch:** Just change `true` ↔ `false`
2. **Clear Intent:** Code shows exactly which backend is active
3. **Easy Debugging:** Logs show active configuration
4. **No Manual URL Changes:** URLs are defined once
5. **Fast Switching:** No need to remember URLs
6. **Safe:** Production flag prevents accidental local usage

### 🔄 Workflow
```
Development:
  USE_LOCAL_BACKEND = true → Build → Test locally

Testing:
  USE_LOCAL_BACKEND = false → Build → Test production

Release:
  USE_LOCAL_BACKEND = false → Build Release → Publish
```

---

## ⚠️ Important Notes

### Before Building Release
**Always check:**
```kotlin
private const val USE_LOCAL_BACKEND = false  // ← MUST BE FALSE!
```

**Why:** Release builds should NEVER use local backend URLs!

### For Physical Device Testing
1. **Find your PC IP:**
   ```powershell
   ipconfig
   ```
   Look for: `IPv4 Address. . . : 192.168.x.x`

2. **Update LOCAL_URL:**
   ```kotlin
   private const val LOCAL_URL = "http://192.168.1.100:3001/"
   ```

3. **Ensure same network:**
   - PC and phone must be on same WiFi
   - Firewall may block connections (disable temporarily)

### Backend Must Be Running
When `USE_LOCAL_BACKEND = true`, make sure:
```powershell
cd D:\HealthBridge\backend
npm start
```

Output should show:
```
🏥 HEALTHBRIDGE API SERVER RUNNING
📍 URL: http://localhost:3001
```

---

## 🛠️ Advanced: Future Enhancement

For even better automation, you could implement:

### Build Variant Switching (Future)
```kotlin
// In build.gradle.kts
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3001/\"")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://mediconnectug.onrender.com/\"")
    }
}

// Then in ApiService.kt
private val BASE_URL = BuildConfig.BASE_URL
```

This would make it **truly automatic** - debug builds always use local, release builds always use production!

---

## 📋 Checklist

### Daily Development
- [ ] Set `USE_LOCAL_BACKEND = true`
- [ ] Start local backend (`npm start`)
- [ ] Build and test

### Testing Production
- [ ] Set `USE_LOCAL_BACKEND = false`
- [ ] Build and test
- [ ] Verify production features work

### Before Release
- [ ] Set `USE_LOCAL_BACKEND = false`
- [ ] Build release APK
- [ ] Test release build
- [ ] Upload to Play Store

---

## 🆘 Troubleshooting

### App Says "Cannot connect to backend"

**If USE_LOCAL_BACKEND = true:**
- Check: Is local backend running?
- Check: Is URL correct for your device type?
- Check: Emulator uses `10.0.2.2`, phone uses PC IP

**If USE_LOCAL_BACKEND = false:**
- Check: Internet connection available?
- Check: Render backend is live?
- Test: `curl https://mediconnectug.onrender.com/`

### Wrong Backend Being Used

**Check the logs:**
```
adb logcat -s ApiClient:*
```

Look for: `Backend URL: ...`

**If wrong:**
- Verify `USE_LOCAL_BACKEND` flag
- Clean and rebuild: `gradlew clean assembleDebug`
- Reinstall APK

###Physical Device Can't Connect to Local Backend

**Solutions:**
1. **Update LOCAL_URL:** Use your PC's IP (not `10.0.2.2`)
2. **Same WiFi:** Ensure phone and PC on same network
3. **Firewall:** Temporarily disable Windows Firewall
4. **Port:** Ensure backend is on port 3001

---

## 📄 Current Configuration

**Status:** ✅ Set to LOCAL DEVELOPMENT

```kotlin
private const val USE_LOCAL_BACKEND = true
private const val LOCAL_URL = "http://10.0.2.2:3001/"
private const val PRODUCTION_URL = "https://mediconnectug.onrender.com/"
```

**This means:**
- App will use: `http://10.0.2.2:3001/`
- Perfect for emulator testing
- Requires local backend running

---

## 🎯 Quick Commands

### Switch to Local
1. Open: `app/src/main/java/com/healthbridge/network/ApiService.kt`
2. Set: `private const val USE_LOCAL_BACKEND = true`
3. Build: `gradlew assembleDebug`

### Switch to Production
1. Open: `app/src/main/java/com/healthbridge/network/ApiService.kt`
2. Set: `private const val USE_LOCAL_BACKEND = false`
3. Build: `gradlew assembleDebug` or `assembleRelease`

### Start Local Backend
```powershell
cd D:\HealthBridge\backend
npm start
```

### Build and Install
```powershell
cd D:\HealthBridge\android_app
.\gradlew assembleDebug
# Then install APK on device/emulator
```

---

**Updated:** May 5, 2026  
**Version:** 2.1.3  
**Status:** ✅ Automatic Backend Switching ENABLED

