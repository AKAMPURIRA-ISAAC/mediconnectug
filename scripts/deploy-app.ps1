#!/usr/bin/env powershell
# ============================================================================
# HealthBridge Android App - Automated Deployment Script
# ============================================================================
# This script automates the build, deployment, and testing process
# Usage: .\scripts\deploy-app.ps1
# ============================================================================

# Set error handling
$ErrorActionPreference = "Stop"

# Colors for output
function Write-Title { Write-Host "`n=== $args ===`n" -ForegroundColor Cyan }
function Write-Success { Write-Host "✅ $args" -ForegroundColor Green }
function Write-Error { Write-Host "❌ $args" -ForegroundColor Red }
function Write-Info { Write-Host "ℹ️  $args" -ForegroundColor Yellow }

Write-Title "HealthBridge Android App - Deployment Script"

# ============================================================================
# PHASE 1: PRE-DEPLOYMENT CHECKS
# ============================================================================
Write-Title "Phase 1: Pre-Deployment Checks"

# Check if gradlew exists
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Error "gradlew.bat not found! Make sure you're in the project root directory."
    exit 1
}
Write-Success "Project directory verified"

# Check if device/emulator connected
$devices = & adb devices | Select-Object -Skip 1 | Where-Object { $_ -and $_ -notmatch "List" }
if ($devices -and ($devices | Measure-Object).Count -gt 0) {
    Write-Success "Android device/emulator connected"
    Write-Info "Connected devices:"
    & adb devices | Select-Object -Skip 1
} else {
    Write-Error "No Android device/emulator detected. Please connect a device or start an emulator."
    exit 1
}

# ============================================================================
# PHASE 2: CLEAN BUILD CACHE
# ============================================================================
Write-Title "Phase 2: Cleaning Build Cache"

Write-Info "Cleaning Gradle cache..."
& .\gradlew.bat cleanBuildCache 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Success "Build cache cleaned successfully"
} else {
    Write-Error "Failed to clean build cache"
    exit 1
}

# ============================================================================
# PHASE 3: BUILD APK
# ============================================================================
Write-Title "Phase 3: Building APK"

Write-Info "Building debug APK (this may take 1-2 minutes)..."
Write-Info "This will:"
Write-Info "  • Compile Kotlin code"
Write-Info "  • Process resources"
Write-Info "  • Package APK"

$buildOutput = & .\gradlew.bat assembleDebug 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Success "APK built successfully"

    # Check APK size
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Info "APK size: $([Math]::Round($apkSize, 2)) MB"

        if ($apkSize -gt 150) {
            Write-Error "APK larger than expected (>150MB). This may cause issues."
        }
    }
} else {
    Write-Error "APK build failed"
    Write-Error $buildOutput
    exit 1
}

# ============================================================================
# PHASE 4: UNINSTALL PREVIOUS VERSION
# ============================================================================
Write-Title "Phase 4: Preparing Device"

Write-Info "Uninstalling previous version..."
& adb uninstall com.healthbridge 2>&1 | Out-Null
Write-Success "Old version uninstalled (or wasn't installed)"

# ============================================================================
# PHASE 5: INSTALL APK
# ============================================================================
Write-Title "Phase 5: Installing APK"

Write-Info "Installing app on device/emulator..."
$installOutput = & adb install -r "app\build\outputs\apk\debug\app-debug.apk" 2>&1

if ($installOutput -match "Success") {
    Write-Success "App installed successfully"
} else {
    Write-Error "Installation failed"
    Write-Error $installOutput
    exit 1
}

# ============================================================================
# PHASE 6: LAUNCH APP
# ============================================================================
Write-Title "Phase 6: Launching App"

Write-Info "Launching HealthBridge..."
& adb shell am start -n "com.healthbridge/.SplashActivity" 2>&1 | Out-Null

Write-Success "App launched!"
Write-Info "Waiting 3 seconds for app to load..."
Start-Sleep -Seconds 3

# ============================================================================
# PHASE 7: VERIFICATION TESTS
# ============================================================================
Write-Title "Phase 7: Initial App Verification"

Write-Info "App should be visible on your device/emulator"
Write-Info "Checking app logs..."

# Capture some log output
$logOutput = & adb logcat -d -s "HealthBridge" 2>&1
if ($logOutput) {
    Write-Success "App is running (logs captured)"
} else {
    Write-Info "No logs captured yet (app may still be loading)"
}

# ============================================================================
# SUMMARY & NEXT STEPS
# ============================================================================
Write-Title "✅ DEPLOYMENT PHASE COMPLETE!"

Write-Host @"
────────────────────────────────────────────────────────────────
✅ DEPLOYMENT SUMMARY
────────────────────────────────────────────────────────────────

What was done:
  ✓ Build cache cleaned
  ✓ Debug APK built successfully
  ✓ App installed on device/emulator
  ✓ App launched

What to do now:
────────────────────────────────────────────────────────────────

1️⃣  TEST DOCTOR REGISTRATION
    • On app, tap "Register as Doctor"
    • Fill all fields
    • Select a hospital/pharmacy from dropdown
    • Complete registration
    • Verify success message

2️⃣  VERIFY DATABASE
    • Go to Supabase SQL Editor
    • Run: SELECT COUNT(*) FROM doctors;
    • Should show 1+ registered doctors

3️⃣  TEST ADDITIONAL FLOWS
    • Try patient registration
    • Test doctor login
    • Navigate through all screens

4️⃣  DATABASE CLEANUP (OPTIONAL)
    If you need a fresh database:
    • Before testing, run: supabase/clean_doctors.sql
    • In Supabase SQL Editor
    • This removes all seeded doctors

────────────────────────────────────────────────────────────────
📋 MANUAL TESTING CHECKLIST
────────────────────────────────────────────────────────────────

Doctor Registration Testing:
  ☐ Red banner shows "Account registration is MANDATORY"
  ☐ Hospital/Pharmacy dropdown populated
  ☐ Pharmacies visible in list (Kampala, Jinja, etc.)
  ☐ "Other" option lets you type custom facility
  ☐ All validations work (missing fields show errors)
  ☐ Registration completes successfully

Functionality Testing:
  ☐ Patient registration works
  ☐ Doctor registration works
  ☐ Doctor login works
  ☐ App doesn't crash during navigation
  ☐ No errors in logcat

────────────────────────────────────────────────────────────────
🚀 NEXT STEPS FOR PRODUCTION
────────────────────────────────────────────────────────────────

1. Complete all manual testing checklist items
2. Run full database cleanup (if applicable)
3. Build release APK: .\scripts\build-release.ps1
4. Upload to Google Play Store OR distribute to users

For detailed instructions, see: DEPLOYMENT_GUIDE.md

────────────────────────────────────────────────────────────────
@ "

Write-Host ""
Write-Success "Ready for manual testing! Check your device/emulator now."
Write-Info "Log file: You can continue monitoring with: adb logcat | findstr HealthBridge"

# ============================================================================
# ADDITIONAL COMMANDS REFERENCE
# ============================================================================
Write-Host @"
────────────────────────────────────────────────────────────────
📝 USEFUL COMMANDS
────────────────────────────────────────────────────────────────

View app logs:
  adb logcat -s "HealthBridge"

Clear app data:
  adb shell pm clear com.healthbridge

Uninstall app:
  adb uninstall com.healthbridge

Restart app:
  adb shell am start -n com.healthbridge/.SplashActivity

View connected devices:
  adb devices

────────────────────────────────────────────────────────────────
" -ForegroundColor Gray

Write-Info "Deployment script completed successfully!"

