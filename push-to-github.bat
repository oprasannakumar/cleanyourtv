@echo off
echo ================================================================
echo Pushing Cleaned CleanYourTV to GitHub
echo Target: https://github.com/oprasannakumar/cleanyourtv.git
echo ================================================================
echo.

git config http.postBuffer 524288000
git config http.lowSpeedLimit 0
git config http.lowSpeedTime 999999

echo Pushing latest commits to GitHub...
git branch -M main
git push -u origin main

if errorlevel 1 (
    echo.
    echo Standard push encountered an issue, trying force push to update main branch...
    git push -u origin main --force
)

echo.
echo ================================================================
echo Push completed!
echo Check your repository at: https://github.com/oprasannakumar/cleanyourtv
echo ================================================================
pause
