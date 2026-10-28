Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Pushing Complete CleanYourTV Repository to GitHub (in 5 chunks)" -ForegroundColor Green
Write-Host "Target: https://github.com/oprasannakumar/cleanyourtv.git" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

git config http.postBuffer 524288000
git config http.lowSpeedLimit 0
git config http.lowSpeedTime 999999

Write-Host "[1/5] Pushing chunk 1/4 (historical commits)..." -ForegroundColor Cyan
git push origin e7b5f4596e549f07e29e846825921b4c87084642:refs/heads/main
if ($LASTEXITCODE -ne 0) {
    Write-Host "Chunk 1 failed. Please ensure you are logged in to GitHub." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/5] Pushing chunk 2/4..." -ForegroundColor Cyan
git push origin 144b3b3efe3a2e35c4dc62d30d96ca2075615a4f:refs/heads/main

Write-Host ""
Write-Host "[3/5] Pushing chunk 3/4..." -ForegroundColor Cyan
git push origin 3b675a8009256f6d9d0944cb34b0563f373f8154:refs/heads/main

Write-Host ""
Write-Host "[4/5] Pushing chunk 4/4 (base launcher)..." -ForegroundColor Cyan
git push origin 1684678064aa7bbb4881e6069e4cfbd4b5fc9df7:refs/heads/main

Write-Host ""
Write-Host "[5/5] Pushing final customized version (all features, zero secrets)..." -ForegroundColor Cyan
git branch -M main
git push -u origin main

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "Push Complete! Visit your GitHub repo:" -ForegroundColor Green
Write-Host "https://github.com/oprasannakumar/cleanyourtv" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Green
