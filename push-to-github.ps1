cd D:\HealthBridge\android_app
.\push-to-github.ps1 -GitHubUsername YOUR-GITHUB-USERNAMEcd D:\HealthBridge\android_app
.\push-to-github.ps1 -GitHubUsername YOUR-GITHUB-USERNAMEparam(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUsername,
    [string]$RepoName = "mediconnectug"
)

$ErrorActionPreference = "Stop"
$RepoUrl = "https://github.com/$GitHubUsername/$RepoName.git"

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   MediConnectUG - Pushing to GitHub"              -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/3] Setting GitHub remote: $RepoUrl" -ForegroundColor Yellow
git remote remove origin 2>$null
git remote add origin $RepoUrl

Write-Host "[2/3] Setting branch to main..." -ForegroundColor Yellow
git branch -M main

Write-Host "[3/3] Pushing to GitHub..." -ForegroundColor Yellow
git push -u origin main
git push --tags

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  CODE IS ON GITHUB!"                              -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Cyan
Write-Host ""
Write-Host "(1) SUPABASE - database (5 min):" -ForegroundColor White
Write-Host "    https://supabase.com -> New Project" -ForegroundColor Gray
Write-Host "    SQL Editor -> paste supabase/schema.sql -> Run" -ForegroundColor Gray
Write-Host "    Settings -> Database -> copy connection string" -ForegroundColor Gray
Write-Host ""
Write-Host "(2) RENDER - backend server (5 min):" -ForegroundColor White
Write-Host "    https://render.com -> New Web Service" -ForegroundColor Gray
Write-Host "    Connect GitHub -> pick $RepoName" -ForegroundColor Gray
Write-Host "    Root Directory: backend   Start: node server.js" -ForegroundColor Gray
Write-Host "    Add env vars: DATABASE_URL + JWT_SECRET" -ForegroundColor Gray
Write-Host ""
Write-Host "(3) GITHUB PAGES - download page (2 min):" -ForegroundColor White
Write-Host "    Repo Settings -> Pages -> main branch -> /docs" -ForegroundColor Gray
Write-Host "    Your page: https://$GitHubUsername.github.io/$RepoName" -ForegroundColor Gray
Write-Host ""
Write-Host "(4) BUILD APK in Android Studio:" -ForegroundColor White
Write-Host "    Build -> Build APK(s)" -ForegroundColor Gray
Write-Host "    Share the APK file via WhatsApp" -ForegroundColor Gray
Write-Host ""
Write-Host "MediConnectUG is LIVE!" -ForegroundColor Green
Write-Host ""

