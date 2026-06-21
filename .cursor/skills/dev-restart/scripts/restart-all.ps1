# restart-all.ps1 — 一键重启前后端
$ErrorActionPreference = "Continue"

$SCRIPT_DIR = Split-Path -Parent $PSCommandPath

Write-Host "=== 后端 ==="
& "$SCRIPT_DIR\restart-backend.ps1"

Write-Host ""
Write-Host "=== 前端 ==="
& "$SCRIPT_DIR\restart-frontend.ps1"

Write-Host ""
Write-Host "全部完成!"
Write-Host "  后端: http://localhost:8000"
Write-Host "  前端: http://localhost:3000"
Write-Host "  数据库管理: http://localhost:8080"
