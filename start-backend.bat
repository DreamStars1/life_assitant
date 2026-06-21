@echo off
REM Life Assistant backend startup script
REM Prerequisite: docker compose -f compose.dev.yml up -d
REM Usage:
REM   start-backend           direct start (jar must exist)
REM   start-backend --build   rebuild then start
REM   start-backend -b        same as --build

setlocal
set ROOT=%~dp0

if /i "%1"=="--build" goto build
if /i "%1"=="-b" goto build

REM direct start, check jar
if not exist "%ROOT%backend\lifeassistant\lifeassistant-server\target\app\bin\lifeassistant.jar" (
    echo [ERROR] JAR not found, run with --build first
    pause
    exit /b 1
)
goto start

:build
echo === Maven package ===
pushd "%ROOT%backend\lifeassistant"
call mvn package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed
    popd
    pause
    exit /b 1
)
popd
echo Build done

:start
echo === Starting backend ===
set DB_HOST=localhost
set DB_PORT=33062
set DB_NAME=app
set DB_USER=app
set DB_PWD=changethis
set REDIS_HOST=localhost
set REDIS_PORT=6379
set REDIS_PWD=
set PROFILES_ACTIVE=dev

cd /d "%ROOT%backend\lifeassistant\lifeassistant-server\target\app"
"%JAVA_HOME%\bin\java.exe" -jar bin\lifeassistant.jar
pause
