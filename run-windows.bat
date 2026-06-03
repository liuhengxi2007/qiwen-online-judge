@echo off
setlocal

set "MINIO_EXE=C:\minio\minio.exe"
set "MINIO_DATA_DIR=%~dp0minio-data"

if not exist "%MINIO_DATA_DIR%" mkdir "%MINIO_DATA_DIR%"

netstat -ano | findstr /R /C:":9000 .*LISTENING" >nul
if not errorlevel 1 (
  echo MinIO appears to already be running on 127.0.0.1:9000; skipping MinIO startup.
) else if exist "%MINIO_EXE%" (
  echo Starting MinIO...
  start "Qiwen MinIO" cmd /k "set MINIO_ROOT_USER=minioadmin&& set MINIO_ROOT_PASSWORD=minioadmin&& ""%MINIO_EXE%"" server ""%MINIO_DATA_DIR%"" --address 127.0.0.1:9000 --console-address 127.0.0.1:9001"
  timeout /t 2 /nobreak >nul
) else (
  echo MinIO executable not found at %MINIO_EXE%
  echo Please put minio.exe there or update MINIO_EXE in this script.
)

echo Starting backend...
start "Qiwen Backend" cmd /k "cd /d %~dp0backend && run-windows.bat"

echo Starting frontend...
start "Qiwen Frontend" cmd /k "cd /d %~dp0frontend && run-windows.bat"

echo Starting judger in WSL...
start "Qiwen Judger (WSL)" cmd /k "cd /d %~dp0judger && run-in-wsl.bat"

endlocal
