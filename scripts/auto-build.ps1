# 🔄 Automatic APK Builder with File Watcher
# Watches for code changes and automatically rebuilds the APK

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                                                            ║" -ForegroundColor Cyan
Write-Host "║     🔄 AUTOMATIC APK BUILDER - STARTING                   ║" -ForegroundColor Cyan
Write-Host "║                                                            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

# Configuration
$projectRoot = $PSScriptRoot
$watchPaths = @(
    "$projectRoot\app\src\main\java",
    "$projectRoot\app\src\main\res",
    "$projectRoot\app\src\main\AndroidManifest.xml"
)

# Track last build time
$lastBuildTime = Get-Date
$debounceSeconds = 3  # Wait 3 seconds after last change before building

Write-Host "`n📂 Watching directories:" -ForegroundColor Yellow
foreach ($path in $watchPaths) {
    if (Test-Path $path) {
        Write-Host "   ✅ $path" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  $path (not found)" -ForegroundColor Red
    }
}

Write-Host "`n🔧 Build Configuration:" -ForegroundColor Yellow
Write-Host "   • Auto-rebuild: ENABLED" -ForegroundColor White
Write-Host "   • Debounce delay: $debounceSeconds seconds" -ForegroundColor White
Write-Host "   • Build command: gradlew assembleDebug" -ForegroundColor White

Write-Host "`n💡 Instructions:" -ForegroundColor Cyan
Write-Host "   • Make changes to your code" -ForegroundColor White
Write-Host "   • Save the file" -ForegroundColor White
Write-Host "   • APK will rebuild automatically!" -ForegroundColor White
Write-Host "   • Press Ctrl+C to stop" -ForegroundColor White

Write-Host "`n🚀 Automatic builder is now active..." -ForegroundColor Green
Write-Host "   Waiting for file changes...`n" -ForegroundColor White

# Initial build
Write-Host "🔨 Building initial APK..." -ForegroundColor Cyan
& "$projectRoot\gradlew.bat" assembleDebug --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Initial build complete!`n" -ForegroundColor Green
} else {
    Write-Host "❌ Initial build failed. Check errors above.`n" -ForegroundColor Red
}

# Create file system watcher
$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = "$projectRoot\app\src\main"
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

# Track changes
$changeQueue = [System.Collections.Generic.Queue[DateTime]]::new()
$buildPending = $false

# Define action on file change
$onChange = {
    param($source, $e)

    $extension = [System.IO.Path]::GetExtension($e.FullPath)

    # Only watch relevant files
    if ($extension -match '\.(kt|java|xml)$') {
        $changeTime = Get-Date
        $changeQueue.Enqueue($changeTime)

        $fileName = Split-Path $e.FullPath -Leaf
        Write-Host "📝 File changed: $fileName" -ForegroundColor Yellow

        $script:buildPending = $true
    }
}

# Register events
Register-ObjectEvent -InputObject $watcher -EventName Changed -Action $onChange | Out-Null
Register-ObjectEvent -InputObject $watcher -EventName Created -Action $onChange | Out-Null
Register-ObjectEvent -InputObject $watcher -EventName Deleted -Action $onChange | Out-Null

try {
    # Main loop
    while ($true) {
        Start-Sleep -Milliseconds 500

        if ($buildPending) {
            # Check if enough time has passed since last change
            $now = Get-Date
            $timeSinceLastChange = ($now - $changeQueue.Peek()).TotalSeconds

            if ($timeSinceLastChange -ge $debounceSeconds) {
                # Clear the queue
                $changeQueue.Clear()
                $buildPending = $false

                Write-Host "`n🔨 Rebuilding APK..." -ForegroundColor Cyan
                $buildStart = Get-Date

                # Run gradle build
                $output = & "$projectRoot\gradlew.bat" assembleDebug 2>&1

                if ($LASTEXITCODE -eq 0) {
                    $buildTime = ((Get-Date) - $buildStart).TotalSeconds
                    Write-Host "✅ Build successful! (${buildTime}s)" -ForegroundColor Green
                    Write-Host "📱 APK: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White

                    # Play success sound (optional)
                    [Console]::Beep(800, 200)
                } else {
                    Write-Host "❌ Build failed! Check errors:" -ForegroundColor Red
                    # Show last 10 lines of output
                    $output | Select-Object -Last 10 | ForEach-Object {
                        Write-Host $_ -ForegroundColor Red
                    }

                    # Play error sound
                    [Console]::Beep(400, 500)
                }

                Write-Host "`n🔄 Waiting for more changes...`n" -ForegroundColor White
            }
        }
    }
} finally {
    # Cleanup
    $watcher.EnableRaisingEvents = $false
    $watcher.Dispose()
    Get-EventSubscriber | Where-Object { $_.SourceObject -eq $watcher } | Unregister-Event
    Write-Host "`n👋 Automatic builder stopped." -ForegroundColor Yellow
}

