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
echo   Maven package (local, fat-jar)...
pushd backend\lifeassistant
call mvn package -P fat-jar -DskipTests -B -q
if %errorlevel% neq 0 (
    echo [FAIL] Maven package failed
    popd
    exit /b 1
)
popd
if not exist "backend\lifeassistant\lifeassistant-server\target\lifeassistant.jar" (
    echo [FAIL] lifeassistant.jar not found after Maven build
    exit /b 1
)
copy /Y "backend\lifeassistant\lifeassistant-server\target\lifeassistant.jar" "backend\app.jar" >nul
docker build -t %BACKEND_IMAGE%:%TAG% -f backend/Dockerfile.runtime backend/
set BUILD_ERR=%errorlevel%
del /Q "backend\app.jar" 2>nul
if %BUILD_ERR% neq 0 (
    echo [FAIL] Backend build failed
    exit /b 1
)
echo [OK] Backend built
echo.

echo [2/8] Building frontend...
echo   pnpm build:pro (local)...
pushd front\vue3-vant-mobile
set HUSKY=0
call pnpm install --frozen-lockfile
if %errorlevel% neq 0 (
    echo [FAIL] pnpm install failed
    popd
    exit /b 1
)
call pnpm build:pro
if %errorlevel% neq 0 (
    echo   node_modules may be corrupt, reinstalling...
    rmdir /s /q node_modules 2>nul
    call pnpm install --frozen-lockfile
    if %errorlevel% neq 0 (
        echo [FAIL] pnpm install failed after clean
        popd
        exit /b 1
    )
    call pnpm build:pro
    if %errorlevel% neq 0 (
        echo [FAIL] pnpm build:pro failed
        popd
        exit /b 1
    )
)
popd
if not exist "front\vue3-vant-mobile\dist\index.html" (
    echo [FAIL] dist/index.html not found after frontend build
    exit /b 1
)
docker build -t %FRONTEND_IMAGE%:%TAG% -f front/vue3-vant-mobile/Dockerfile.runtime front/vue3-vant-mobile/
if %errorlevel% neq 0 (
    echo [FAIL] Frontend build failed
    exit /b 1
)
echo [OK] Frontend built
echo.

echo [3/8] Nginx (skip rebuild, use local image)...
docker image inspect %NGINX_IMAGE%:%TAG% >nul 2>&1
if %errorlevel% neq 0 (
    echo [FAIL] Local nginx image not found: %NGINX_IMAGE%:%TAG%
    exit /b 1
)
echo [OK] Nginx ready
echo.

echo [4/8] Agent (skip rebuild, use local image)...
docker image inspect %AGENT_IMAGE%:%TAG% >nul 2>&1
if %errorlevel% neq 0 (
    echo [FAIL] Local agent image not found: %AGENT_IMAGE%:%TAG%
    exit /b 1
)
echo [OK] Agent ready
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
