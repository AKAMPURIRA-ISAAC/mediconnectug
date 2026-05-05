# 🔄 Automatic Build, Install & Restart App
# The ultimate developer convenience script!

$projectRoot = $PSScriptRoot
$packageName = "com.healthbridge"
$apkPath = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║                                                            ║" -ForegroundColor Magenta
Write-Host "║     🚀 BUILD → INSTALL → LAUNCH                           ║" -ForegroundColor Magenta
Write-Host "║                                                            ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Step 1: Build
Write-Host "`n[1/3] 🔨 Building APK..." -ForegroundColor Cyan
$buildStart = Get-Date
& "$projectRoot\gradlew.bat" assembleDebug --quiet

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

$buildTime = [math]::Round(((Get-Date) - $buildStart).TotalSeconds, 1)
Write-Host "✅ Build complete (${buildTime}s)" -ForegroundColor Green

# Step 2: Install
Write-Host "`n[2/3] 📱 Installing APK..." -ForegroundColor Cyan

# Check for devices
$devicesOutput = & adb devices 2>&1 | Out-String
if ($devicesOutput -match "device\s*$" -and $devicesOutput -notmatch "List of devices") {
    Write-Host "   Device detected ✓" -ForegroundColor Green

    & adb install -r $apkPath 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Installation successful" -ForegroundColor Green
    } else {
        Write-Host "❌ Installation failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⚠️  No device connected" -ForegroundColor Yellow
    Write-Host "   APK built but not installed" -ForegroundColor White
    Write-Host "   Location: $apkPath" -ForegroundColor White
    exit 0
}

# Step 3: Launch app
Write-Host "`n[3/3] 🚀 Launching app..." -ForegroundColor Cyan

# Close app if running
& adb shell am force-stop $packageName 2>&1 | Out-Null

# Launch main activity
$launchActivity = "$packageName/.SplashActivity"
& adb shell am start -n $launchActivity 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ App launched successfully" -ForegroundColor Green
    [Console]::Beep(800, 150)
    [Console]::Beep(1000, 150)
} else {
    Write-Host "⚠️  App installed but launch failed" -ForegroundColor Yellow
    Write-Host "   Please launch manually" -ForegroundColor White
}

Write-Host "`n🎉 All done! Your app is running." -ForegroundColor Green
Write-Host "   Build time: ${buildTime}s" -ForegroundColor White
Write-Host "`n💡 Tip: Run .\auto-build.ps1 for continuous builds" -ForegroundColor Cyan
Write-Host "`n"

