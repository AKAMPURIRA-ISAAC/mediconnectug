# Quick Test Script - Message Button Fix
# Run this to test the message button with logging

Write-Host "=== HealthBridge Message Button Test ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if device is connected
Write-Host "[1/5] Checking device connection..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    Write-Host "Please connect your Android device or start an emulator." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Device connected" -ForegroundColor Green
Write-Host ""

# Step 2: Build APK
Write-Host "[2/5] Building APK..." -ForegroundColor Yellow
$buildResult = .\gradlew assembleDebug --console=plain 2>&1 | Select-String "BUILD"
if ($buildResult -match "SUCCESS") {
    Write-Host "✅ Build successful" -ForegroundColor Green
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host "Run: .\gradlew assembleDebug --stacktrace" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 3: Install APK
Write-Host "[3/5] Installing APK..." -ForegroundColor Yellow
adb install -r app\build\outputs\apk\debug\app-debug.apk 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ APK installed" -ForegroundColor Green
} else {
    Write-Host "⚠️  Installation warning (might still work)" -ForegroundColor Yellow
}
Write-Host ""

# Step 4: Clear old logs
Write-Host "[4/5] Clearing old logs..." -ForegroundColor Yellow
adb logcat -c
Write-Host "✅ Logs cleared" -ForegroundColor Green
Write-Host ""

# Step 5: Start monitoring logs
Write-Host "[5/5] Starting log monitor..." -ForegroundColor Yellow
Write-Host ""
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "  WATCHING LOGS - DoctorProfile" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📱 Now:" -ForegroundColor Yellow
Write-Host "  1. Open HealthBridge app on your device" -ForegroundColor White
Write-Host "  2. Find a doctor" -ForegroundColor White
Write-Host "  3. Open doctor profile" -ForegroundColor White
Write-Host "  4. Click 'Message' button" -ForegroundColor White
Write-Host ""
Write-Host "👀 Watch for:" -ForegroundColor Yellow
Write-Host "  • 'Creating chat session' = Good!" -ForegroundColor Green
Write-Host "  • 'API Response: success=true' = Good!" -ForegroundColor Green
Write-Host "  • 'PatientChatActivity started' = PERFECT!" -ForegroundColor Green
Write-Host "  • 'Exception creating chat' = Problem found!" -ForegroundColor Red
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Gray
Write-Host ""

# Monitor logs
adb logcat -s DoctorProfile:* *:E | ForEach-Object {
    $line = $_
    if ($line -match "Creating chat session") {
        Write-Host $line -ForegroundColor Cyan
    } elseif ($line -match "API Response: success=true") {
        Write-Host $line -ForegroundColor Green
    } elseif ($line -match "PatientChatActivity started") {
        Write-Host $line -ForegroundColor Green
        Write-Host ""
        Write-Host "🎉 SUCCESS! Chat opened successfully!" -ForegroundColor Green
        Write-Host ""
    } elseif ($line -match "Exception|Error|Failed") {
        Write-Host $line -ForegroundColor Red
        Write-Host ""
        Write-Host "❌ ERROR FOUND!" -ForegroundColor Red
        Write-Host "Check MESSAGE_BUTTON_TROUBLESHOOTING.md for solutions" -ForegroundColor Yellow
        Write-Host ""
    } else {
        Write-Host $line
    }
}

