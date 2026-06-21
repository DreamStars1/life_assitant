# restart-frontend.ps1 — 重启 Vue3 Vite 前端
$ErrorActionPreference = "Stop"

$PROJECT_ROOT = Resolve-Path "$PSScriptRoot\..\..\..\.."
$FRONTEND_DIR = "$PROJECT_ROOT\front\vue3-vant-mobile"

Write-Host "=== 1/2 停止旧前端 ==="
$old = Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue
if ($old) {
    Stop-Process -Id $old.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "  已停止 PID $($old.OwningProcess)"
} else { Write-Host "  无运行中的前端" }

Write-Host "=== 2/2 启动前端 (端口 3000) ==="
Push-Location $FRONTEND_DIR
try {
    pnpm dev 2>&1 &
    Start-Sleep -Seconds 5
    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt 30) {
        $port = Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue
        if ($port) {
            Write-Host "  前端已启动: http://localhost:3000"
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $port) {
        Write-Host "  WARN: 前端 30s 内未就绪，请检查终端输出"
    }
} finally { Pop-Location }

Write-Host "完成!"
