Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Pushing Cleaned CleanYourTV to GitHub" -ForegroundColor Green
Write-Host "Target: https://github.com/oprasannakumar/cleanyourtv.git" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

git config http.postBuffer 524288000
git config http.lowSpeedLimit 0
git config http.lowSpeedTime 999999

Write-Host "Pushing commits to GitHub..." -ForegroundColor Cyan
git branch -M main
git push -u origin main

if ($LASTEXITCODE -ne 0) {
    Write-Host "Trying force push to ensure rewritten sanitized commits update main..." -ForegroundColor Yellow
    git push -u origin main --force
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "Push completed! Visit: https://github.com/oprasannakumar/cleanyourtv" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
