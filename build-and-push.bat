@echo off
setlocal

set REGISTRY=ccr.ccs.tencentyun.com/life-assistant
set BACKEND_IMAGE=%REGISTRY%/lifeassistant-backend
set FRONTEND_IMAGE=%REGISTRY%/lifeassistant-frontend
set NGINX_IMAGE=%REGISTRY%/lifeassistant-nginx
set AGENT_IMAGE=%REGISTRY%/lifeassistant-agent
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

echo [1/8] Building backend...
docker build -t %BACKEND_IMAGE%:%TAG% -f backend/Dockerfile backend/
if %errorlevel% neq 0 (
    echo [FAIL] Backend build failed
    exit /b 1
)
echo [OK] Backend built
echo.

echo [2/8] Building frontend...
docker build -t %FRONTEND_IMAGE%:%TAG% -f front/vue3-vant-mobile/Dockerfile front/vue3-vant-mobile/
if %errorlevel% neq 0 (
    echo [FAIL] Frontend build failed
    exit /b 1
)
echo [OK] Frontend built
echo.

echo [3/8] Building nginx...
docker build -t %NGINX_IMAGE%:%TAG% -f backend/nginx/Dockerfile backend/nginx/
if %errorlevel% neq 0 (
    echo [FAIL] Nginx build failed
    exit /b 1
)
echo [OK] Nginx built
echo.

echo [4/8] Building agent...
docker build -t %AGENT_IMAGE%:%TAG% -f python/agent/Dockerfile python/agent/
if %errorlevel% neq 0 (
    echo [FAIL] Agent build failed
    exit /b 1
)
echo [OK] Agent built
echo.

echo [5/8] Pushing backend...
docker push %BACKEND_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Backend pushed
echo.

echo [6/8] Pushing frontend...
docker push %FRONTEND_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Frontend pushed
echo.

echo [7/8] Pushing nginx...
docker push %NGINX_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Nginx pushed
echo.

echo [8/8] Pushing agent...
docker push %AGENT_IMAGE%:%TAG%
if %errorlevel% neq 0 (
    echo [FAIL] Push failed, check docker login
    exit /b 1
)
echo [OK] Agent pushed
echo.

echo ========================================
echo  Done!
echo  %BACKEND_IMAGE%:%TAG%
echo  %FRONTEND_IMAGE%:%TAG%
echo  %NGINX_IMAGE%:%TAG%
echo  %AGENT_IMAGE%:%TAG%
echo ========================================
endlocal
