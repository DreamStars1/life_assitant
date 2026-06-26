@echo off
setlocal

set REGISTRY=ccr.ccs.tencentyun.com/life-assistant
set BACKEND_IMAGE=%REGISTRY%/lifeassistant-backend
set FRONTEND_IMAGE=%REGISTRY%/lifeassistant-frontend
set NGINX_IMAGE=%REGISTRY%/lifeassistant-nginx
set TAG=latest

echo ========================================
echo  Build and Push: %REGISTRY%
echo ========================================
echo.

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [FAIL] Docker daemon is not running. Start Docker Desktop first.
    exit /b 1
)

echo [1/6] Building backend...
docker build -t %BACKEND_IMAGE%:%TAG% -f backend/Dockerfile backend/
if %errorlevel% neq 0 (
    echo [FAIL] Backend build failed
    exit /b 1
)
echo [OK] Backend built
echo.

echo [2/6] Building frontend...
docker build -t %FRONTEND_IMAGE%:%TAG% -f front/vue3-vant-mobile/Dockerfile front/vue3-vant-mobile/
if %errorlevel% neq 0 (
    echo [FAIL] Frontend build failed
    exit /b 1
)
echo [OK] Frontend built
echo.

echo [3/6] Building nginx...
docker build -t %NGINX_IMAGE%:%TAG% -f backend/nginx/Dockerfile backend/nginx/
if %errorlevel% neq 0 (
    echo [FAIL] Nginx build failed
    exit /b 1
)
echo [OK] Nginx built
echo.

echo [4/6] Pushing backend...
docker push %BACKEND_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Backend pushed
echo.

echo [5/6] Pushing frontend...
docker push %FRONTEND_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Frontend pushed
echo.

echo [6/6] Pushing nginx...
docker push %NGINX_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Nginx pushed
echo.

echo ========================================
echo  Done!
echo  %BACKEND_IMAGE%:%TAG%
echo  %FRONTEND_IMAGE%:%TAG%
echo  %NGINX_IMAGE%:%TAG%
echo ========================================
endlocal
