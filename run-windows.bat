@echo off
setlocal

echo Starting backend...
start "Qiwen Backend" cmd /k "cd /d %~dp0backend && run-windows.bat"

echo Starting frontend...
start "Qiwen Frontend" cmd /k "cd /d %~dp0frontend && run-windows.bat"

endlocal
