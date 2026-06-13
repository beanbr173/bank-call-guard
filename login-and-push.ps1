# One-time GitHub login + push for Bank Call Guard
$ErrorActionPreference = "Stop"

$gh = "C:\Program Files\GitHub CLI\gh.exe"
$repo = "D:\working\play_store_apps\bank_call_guard"

if (-not (Test-Path $gh)) {
    Write-Host "GitHub CLI not found at: $gh" -ForegroundColor Red
    exit 1
}

$env:Path = "C:\Program Files\GitHub CLI;" + $env:Path
Set-Location $repo

Write-Host ""
Write-Host "=== Step 1: GitHub login (browser) ===" -ForegroundColor Cyan
Write-Host "Choose: GitHub.com -> HTTPS -> Login with a web browser"
Write-Host ""

& $gh auth login

Write-Host ""
Write-Host "=== Step 2: Connect Git to GitHub ===" -ForegroundColor Cyan
& $gh auth setup-git

Write-Host ""
Write-Host "=== Step 3: Push to origin/main ===" -ForegroundColor Cyan
git push -u origin main

Write-Host ""
Write-Host "Done. Check build status:" -ForegroundColor Green
Write-Host "https://github.com/beanbr173/bank-call-guard/actions"
Write-Host ""
