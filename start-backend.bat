@echo off
REM Life Assistant 后端启动脚本
REM 前置条件：已通过 docker compose -f compose.dev.yml up -d 启动 MySQL 和 Redis
REM 所需环境变量参见 .env.example

cd /d "%~dp0backend\lifeassistant\lifeassistant-server\target\app"
"%JAVA_HOME%\bin\java.exe" -jar bin\lifeassistant.jar
pause
