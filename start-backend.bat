@echo off
REM Life Assistant 后端启动脚本
REM 前置条件：已通过 docker compose -f compose.dev.yml up -d 启动 MySQL 和 Redis
REM 所需环境变量参见 .env.example
REM 用法：
REM   start-backend             直接启动（需先构建过）
REM   start-backend --build     重新打包后再启动

setlocal
set ROOT=%~dp0

if /i "%1"=="--build" goto build
if /i "%1"=="-b" goto build

REM 直接启动
if not exist "%ROOT%backend\lifeassistant\lifeassistant-server\target\app\bin\lifeassistant.jar" (
    echo [ERROR] JAR 不存在，请先执行 start-backend --build
    pause
    exit /b 1
)
goto start

:build
echo === 重新打包后端 ===
pushd "%ROOT%backend\lifeassistant"
call mvn package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven 构建失败
    popd
    pause
    exit /b 1
)
popd
echo 打包完成

:start
echo === 启动后端 ===
cd /d "%ROOT%backend\lifeassistant\lifeassistant-server\target\app"
"%JAVA_HOME%\bin\java.exe" -jar bin\lifeassistant.jar
pause
