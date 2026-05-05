# 🚀 Quick Build and Auto-Install APK
# Builds APK and automatically installs it on connected device/emulator

param(
    [switch]$Watch,  # Enable continuous watching
    [switch]$Install  # Auto-install after build
)

$projectRoot = $PSScriptRoot

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                                                            ║" -ForegroundColor Green
Write-Host "║     ⚡ QUICK BUILD & INSTALL                              ║" -ForegroundColor Green
Write-Host "║                                                            ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green

# Function to build APK
function Build-APK {
    Write-Host "`n🔨 Building APK..." -ForegroundColor Cyan
    $buildStart = Get-Date

    $result = & "$projectRoot\gradlew.bat" assembleDebug 2>&1

    if ($LASTEXITCODE -eq 0) {
        $buildTime = [math]::Round(((Get-Date) - $buildStart).TotalSeconds, 1)
        Write-Host "✅ Build successful! (${buildTime}s)" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ Build failed!" -ForegroundColor Red
        $result | Select-Object -Last 15 | ForEach-Object {
            Write-Host $_ -ForegroundColor Red
        }
        return $false
    }
}

# Function to install APK
function Install-APK {
    $apkPath = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"

    if (-not (Test-Path $apkPath)) {
        Write-Host "❌ APK not found: $apkPath" -ForegroundColor Red
        return $false
    }

    Write-Host "`n📱 Installing APK..." -ForegroundColor Cyan

    # Check for connected devices
    $devices = & adb devices 2>&1 | Select-String -Pattern "device$" -NotMatch "List"

    if ($devices.Count -eq 0) {
        Write-Host "⚠️  No devices connected" -ForegroundColor Yellow
        Write-Host "   Please connect a device or start an emulator" -ForegroundColor White
        return $false
    }

    Write-Host "   Installing on device..." -ForegroundColor White
    $installResult = & adb install -r $apkPath 2>&1

    if ($installResult -match "Success") {
        Write-Host "✅ Installation successful!" -ForegroundColor Green
        [Console]::Beep(800, 200)
        return $true
    } else {
        Write-Host "❌ Installation failed:" -ForegroundColor Red
        Write-Host $installResult -ForegroundColor Red
        return $false
    }
}

# Main execution
if ($Watch) {
    Write-Host "`n🔄 Starting auto-build watcher..." -ForegroundColor Cyan
    Write-Host "   (This will rebuild on every code change)" -ForegroundColor White
    & "$projectRoot\auto-build.ps1"
} else {
    # Single build
    $buildSuccess = Build-APK

    if ($buildSuccess) {
        Write-Host "`n📦 APK Location:" -ForegroundColor Cyan
        Write-Host "   app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White

        if ($Install) {
            $installSuccess = Install-APK

            if ($installSuccess) {
                Write-Host "`n🎉 Ready to test!" -ForegroundColor Green
            }
        } else {
            Write-Host "`n💡 Tip: Use -Install flag to auto-install" -ForegroundColor Yellow
            Write-Host "   Example: .\quick-build.ps1 -Install" -ForegroundColor White
        }
    }

    Write-Host "`n"
}

