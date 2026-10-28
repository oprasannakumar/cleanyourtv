@echo off
echo ================================================================
echo Pushing Complete CleanYourTV Repository to GitHub (in 5 chunks)
echo Target: https://github.com/oprasannakumar/cleanyourtv.git
echo ================================================================
echo.

git config http.postBuffer 524288000
git config http.lowSpeedLimit 0
git config http.lowSpeedTime 999999

echo [1/5] Pushing chunk 1/4...
git push origin e7b5f4596e549f07e29e846825921b4c87084642:refs/heads/main
if errorlevel 1 (
    echo Chunk 1 failed. Please check your GitHub connection.
    pause
    exit /b 1
)

echo.
echo [2/5] Pushing chunk 2/4...
git push origin 144b3b3efe3a2e35c4dc62d30d96ca2075615a4f:refs/heads/main

echo.
echo [3/5] Pushing chunk 3/4...
git push origin 3b675a8009256f6d9d0944cb34b0563f373f8154:refs/heads/main

echo.
echo [4/5] Pushing chunk 4/4...
git push origin 1684678064aa7bbb4881e6069e4cfbd4b5fc9df7:refs/heads/main

echo.
echo [5/5] Pushing final customized version (all features, zero secrets)...
git branch -M main
git push -u origin main

echo.
echo ================================================================
echo Push completed!
echo Repository: https://github.com/oprasannakumar/cleanyourtv
echo ================================================================
pause
