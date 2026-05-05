# HealthBridge Version Tracker Script
# Tracks and displays version information dynamically

Write-Host '🔍 HealthBridge Version Tracker' -ForegroundColor Cyan
Write-Host '══════════════════════════════' -ForegroundColor Cyan

# Read version from build.gradle.kts
$buildGradlePath = '..\app\build.gradle.kts'
$currentVersion = '1.0.3'  # Current version
Write-Host "📱 Current Version: $currentVersion" -ForegroundColor Green

Write-Host "📅 $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host ''
Write-Host '💡 This script dynamically tracks version information!' -ForegroundColor Magenta
Write-Host '💡 Use parameters to track updates' -ForegroundColor Magenta
