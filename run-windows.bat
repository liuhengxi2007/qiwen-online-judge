@echo off
setlocal

echo Starting backend...
start "Qiwen Backend" cmd /k "cd /d %~dp0backend && run-windows.bat"

echo Starting frontend...
start "Qiwen Frontend" cmd /k "cd /d %~dp0frontend && run-windows.bat"

echo Starting judger in WSL...
start "Qiwen Judger (WSL)" cmd /k "cd /d %~dp0judger && set JUDGER_START_DELAY_SECONDS=10 && run-in-wsl.bat"

endlocal
