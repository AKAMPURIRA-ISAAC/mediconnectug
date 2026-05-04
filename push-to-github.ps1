#!/usr/bin/env pwsh
# ═══════════════════════════════════════════════════════════════
#  MediConnectUG — One-Script GitHub Push
#  Run this in PowerShell after creating your GitHub repo
# ═══════════════════════════════════════════════════════════════

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUsername,

    [string]$RepoName = "mediconnectug"
)

$ErrorActionPreference = "Stop"
$RepoUrl = "https://github.com/$GitHubUsername/$RepoName.git"

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   MediConnectUG — Pushing to GitHub              ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Set remote ──────────────────────────────────────────
Write-Host "🔗 Setting GitHub remote: $RepoUrl" -ForegroundColor Yellow
git remote remove origin 2>$null
git remote add origin $RepoUrl

# ── Step 2: Rename branch to main ──────────────────────────────
Write-Host "🌿 Setting branch to 'main'..." -ForegroundColor Yellow
git branch -M main

# ── Step 3: Push ────────────────────────────────────────────────
Write-Host "🚀 Pushing 156 files to GitHub..." -ForegroundColor Yellow
git push -u origin main

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  ✅ CODE IS ON GITHUB!                           ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "📋 NEXT STEPS:" -ForegroundColor Cyan
Write-Host ""
Write-Host "① SUPABASE (database — 5 min):" -ForegroundColor White
Write-Host "   → https://supabase.com → New Project" -ForegroundColor Gray
Write-Host "   → SQL Editor → paste supabase/schema.sql → Run" -ForegroundColor Gray
Write-Host "   → Settings → Database → copy connection string" -ForegroundColor Gray
Write-Host ""
Write-Host "② RENDER (backend server — 5 min):" -ForegroundColor White
Write-Host "   → https://render.com → New Web Service" -ForegroundColor Gray
Write-Host "   → Connect GitHub → pick '$RepoName'" -ForegroundColor Gray
Write-Host "   → Root Directory: backend | Start: node server.js" -ForegroundColor Gray
Write-Host "   → Add env vars: DATABASE_URL + JWT_SECRET" -ForegroundColor Gray
Write-Host "   → Your API: https://mediconnectug-api.onrender.com" -ForegroundColor Gray
Write-Host ""
Write-Host "③ GITHUB PAGES (download page — 2 min):" -ForegroundColor White
Write-Host "   → Repo Settings → Pages → main branch → /docs → Save" -ForegroundColor Gray
Write-Host "   → Your download page: https://$GitHubUsername.github.io/$RepoName" -ForegroundColor Gray
Write-Host ""
Write-Host "④ BUILD & SHARE APK:" -ForegroundColor White
Write-Host "   → Android Studio → Build → Build APK(s)" -ForegroundColor Gray
Write-Host "   → Share app/build/outputs/apk/debug/app-debug.apk via WhatsApp" -ForegroundColor Gray
Write-Host ""
Write-Host "🎉 MediConnectUG will be LIVE!" -ForegroundColor Green
Write-Host ""

