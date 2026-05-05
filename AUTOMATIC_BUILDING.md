# 🚀 Automatic APK Building - Complete Guide

## ✅ What Was Implemented

You now have **THREE automatic build scripts** that make development super fast!

---

## 📜 Available Scripts

### 1. 🔄 `auto-build.ps1` - Continuous Auto-Builder
**Best for:** Active development when making multiple changes

**What it does:**
- Watches your code files for changes
- Automatically rebuilds APK when you save
- Debounces (waits 3s) to avoid building too often
- Shows real-time build status
- Plays sound on success/failure

**Usage:**
```powershell
cd D:\HealthBridge\android_app
.\auto-build.ps1
```

**Then:**
1. Edit any `.kt`, `.java`, or `.xml` file
2. Save the file
3. Wait 3 seconds
4. APK automatically rebuilds!
5. Press Ctrl+C to stop

---

### 2. ⚡ `quick-build.ps1` - Fast Single Build
**Best for:** Quick one-time builds

**What it does:**
- Builds APK quickly
- Optionally installs on device
- Shows build time
- Checks for connected devices

**Usage:**
```powershell
# Just build
.\quick-build.ps1

# Build and install
.\quick-build.ps1 -Install

# Enable watch mode (same as auto-build.ps1)
.\quick-build.ps1 -Watch
```

---

### 3. 🚀 `build-install-run.ps1` - Complete Workflow
**Best for:** Full testing workflow

**What it does:**
1. Builds APK
2. Installs on connected device/emulator
3. Closes old app instance
4. Launches app automatically
5. Shows success with sound

**Usage:**
```powershell
.\build-install-run.ps1
```

**Perfect for:** Testing your changes immediately!

---

## 🎯 Recommended Workflows

### Daily Development Workflow
```powershell
# Terminal 1: Start local backend
cd D:\HealthBridge\backend
npm start

# Terminal 2: Start auto-builder
cd D:\HealthBridge\android_app
.\auto-build.ps1

# Now just code! APK rebuilds automatically on save
```

### Quick Test Workflow
```powershell
# Make changes, then:
.\build-install-run.ps1

# App builds, installs, and launches automatically!
```

### Single Build Workflow
```powershell
# Just need a fresh APK:
.\quick-build.ps1

# Or build and install:
.\quick-build.ps1 -Install
```

---

## 🔍 Script Details

### auto-build.ps1 Features
```
✅ Watches: .kt, .java, .xml files
✅ Debounce: 3-second delay
✅ Auto-rebuild: On file save
✅ Status display: Real-time
✅ Sound alerts: Success/failure
✅ Error display: Last 10 lines
```

**Watched directories:**
- `app/src/main/java/**`
- `app/src/main/res/**`
- `app/src/main/AndroidManifest.xml`

**Terminal output:**
```
🔄 AUTOMATIC APK BUILDER - STARTING

📂 Watching directories:
   ✅ D:\HealthBridge\android_app\app\src\main\java
   ✅ D:\HealthBridge\android_app\app\src\main\res

🚀 Automatic builder is now active...

📝 File changed: ApiService.kt
🔨 Rebuilding APK...
✅ Build successful! (8.3s)
📱 APK: app\build\outputs\apk\debug\app-debug.apk

🔄 Waiting for more changes...
```

---

### quick-build.ps1 Features
```
✅ Fast single builds
✅ Optional auto-install
✅ Device detection
✅ Build time tracking
✅ Clean output
```

**Example output:**
```
⚡ QUICK BUILD & INSTALL

🔨 Building APK...
✅ Build successful! (9.2s)

📦 APK Location:
   app\build\outputs\apk\debug\app-debug.apk

💡 Tip: Use -Install flag to auto-install
```

---

### build-install-run.ps1 Features
```
✅ Complete workflow automation
✅ Build → Install → Launch
✅ Closes old app instance
✅ Auto-launches new version
✅ Sound confirmation
```

**Example output:**
```
🚀 BUILD → INSTALL → LAUNCH

[1/3] 🔨 Building APK...
✅ Build complete (8.7s)

[2/3] 📱 Installing APK...
   Device detected ✓
✅ Installation successful

[3/3] 🚀 Launching app...
✅ App launched successfully

🎉 All done! Your app is running.
```

---

## 💡 Pro Tips

### Tip 1: Keep Auto-Build Running
```powershell
# Open PowerShell, start auto-builder, minimize window
.\auto-build.ps1

# Now code normally - builds happen in background!
```

### Tip 2: Quick Install After Edit
```powershell
# After making changes:
.\build-install-run.ps1

# Gets app on device in one command!
```

### Tip 3: Check Build Without Installing
```powershell
# Just verify code compiles:
.\quick-build.ps1

# No device needed, just checks for errors
```

### Tip 4: Multiple Terminals
```
Terminal 1: Backend (npm start)
Terminal 2: Auto-builder (.\auto-build.ps1)  
Terminal 3: Available for commands
```

---

## ⚙️ Configuration

### Debounce Time (auto-build.ps1)
```powershell
# Line 12: Adjust wait time after file change
$debounceSeconds = 3  # Default: 3 seconds
```

**Too fast?** Increase to 5-10 seconds
**Too slow?** Decrease to 1-2 seconds

### Watch Additional Files
```powershell
# Line 7-11: Add more paths
$watchPaths = @(
    "$projectRoot\app\src\main\java",
    "$projectRoot\app\src\main\res",
    "$projectRoot\app\src\main\AndroidManifest.xml",
    "$projectRoot\app\build.gradle.kts"  # Example: watch build file
)
```

### Disable Sound Alerts
```powershell
# Comment out these lines:
# [Console]::Beep(800, 200)  # Success
# [Console]::Beep(400, 500)  # Error
```

---

## 🆘 Troubleshooting

### Script Won't Run
**Error:** "running scripts is disabled"

**Fix:**
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Auto-Build Not Detecting Changes
**Possible causes:**
1. File saved outside watched directories
2. IDE not saving file (check auto-save)
3. Script stopped or crashed

**Fix:** Restart script, check file paths

### Build Fails Continuously
**Fix:**
```powershell
# Stop auto-builder (Ctrl+C)
# Fix code errors
# Clean build
.\gradlew clean
# Restart auto-builder
.\auto-build.ps1
```

### APK Won't Install
**Possible causes:**
1. No device connected
2. Old version conflicts
3. ADB not in PATH

**Fix:**
```powershell
# Check devices
adb devices

# Uninstall old version
adb uninstall com.healthbridge

# Try again
.\build-install-run.ps1
```

---

## 📊 Performance Comparison

### Manual Workflow (Before)
```
1. Edit code
2. Save file
3. Open terminal
4. Type: .\gradlew assembleDebug
5. Wait for build
6. Open File Explorer
7. Find APK
8. Drag to emulator
   Total: ~2-3 minutes per test
```

### Automatic Workflow (Now)
```
1. Edit code
2. Save file
3. Wait 3 seconds
4. APK ready!
   Total: ~10 seconds per test
```

**Time saved:** ~2 minutes per build × 20 builds/day = **40 minutes saved daily!**

---

## 🔗 Integration with Backend Switching

Works perfectly with automatic backend switching!

```powershell
# Edit ApiService.kt
private const val USE_LOCAL_BACKEND = true  # Local dev

# Save file → Auto-builder rebuilds immediately!
# No manual commands needed
```

---

## 🎯 Quick Reference

| Task | Command | Time | Auto-Install | Auto-Launch |
|------|---------|------|--------------|-------------|
| Continuous builds | `.\auto-build.ps1` | N/A | ❌ | ❌ |
| Quick single build | `.\quick-build.ps1` | ~10s | ❌ | ❌ |
| Build + Install | `.\quick-build.ps1 -Install` | ~15s | ✅ | ❌ |
| Full workflow | `.\build-install-run.ps1` | ~20s | ✅ | ✅ |

---

## 🚀 Best Practice Setup

### Recommended Terminal Layout

**PowerShell Tab 1: Backend**
```powershell
cd D:\HealthBridge\backend
npm start
```

**PowerShell Tab 2: Auto-Builder**
```powershell
cd D:\HealthBridge\android_app
.\auto-build.ps1
```

**PowerShell Tab 3: Commands**
```powershell
cd D:\HealthBridge\android_app
# Available for one-off commands
```

### Development Flow
```
1. Start backend (Tab 1)
2. Start auto-builder (Tab 2)
3. Open Android Studio
4. Edit code
5. Save (Ctrl+S)
6. Watch Tab 2 - APK builds automatically!
7. When ready to test: .\build-install-run.ps1 (Tab 3)
8. App installs and launches automatically!
```

---

## 🎉 Summary

You now have **three powerful scripts** that eliminate manual build steps:

1. **auto-build.ps1** → Continuous background building
2. **quick-build.ps1** → Fast single builds with optional install
3. **build-install-run.ps1** → Complete test workflow automation

**Benefits:**
- ⚡ Saves 40+ minutes daily
- 🔄 Automatic rebuilds on save
- 📱 One-command install + launch
- 🎯 Focus on coding, not building
- ✅ Fewer manual steps
- 🚀 Faster development cycle

**Start using them now:**
```powershell
cd D:\HealthBridge\android_app
.\auto-build.ps1
```

---

**Updated:** May 5, 2026  
**Version:** 2.1.3  
**Status:** ✅ Automatic Building ENABLED

