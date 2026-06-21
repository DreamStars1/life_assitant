# restart-backend.ps1 — 重新构建并启动 Java Spring Boot 后端
$ErrorActionPreference = "Stop"

$PROJECT_ROOT = Resolve-Path "$PSScriptRoot\..\..\..\.."
$BACKEND_DIR = "$PROJECT_ROOT\backend\lifeassistant"
$JAVA = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "java" }
$JAR_DIR = "$BACKEND_DIR\lifeassistant-server\target\app"

Write-Host "=== 1/3 停止旧后端 ==="
$old = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($old) {
    Stop-Process -Id $old.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    Write-Host "  已停止 PID $($old.OwningProcess)"
} else { Write-Host "  无运行中的后端" }

Write-Host "=== 2/3 构建后端（如需要）==="
if (-not (Test-Path "$JAR_DIR\bin\lifeassistant.jar")) {
    Push-Location $BACKEND_DIR
    try {
        mvn package -DskipTests 2>&1 | Select-String "BUILD|ERROR"
        if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败" }
    } catch { throw } finally { Pop-Location }
} else { Write-Host "  JAR 已存在，跳过构建" }

Write-Host "=== 3/3 启动后端 ==="
Push-Location $JAR_DIR
Start-Process -NoNewWindow $JAVA -ArgumentList '-jar', 'bin\lifeassistant.jar'
Start-Sleep -Seconds 15
$start = Get-Date
while (((Get-Date) - $start).TotalSeconds -lt 60) {
    $port = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
    if ($port) {
        Write-Host "  后端已启动: http://localhost:8000"
        break
    }
    Start-Sleep -Seconds 3
}
if (-not $port) { Write-Host "  WARN: 60s 未就绪" }

Write-Host "完成!"
