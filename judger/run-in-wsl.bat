@echo off
setlocal

for %%I in ("%~dp0.") do set "JUDGER_WIN_PATH=%%~fI"
set "JUDGER_WIN_PATH=%JUDGER_WIN_PATH:\=/%"
if "%BACKEND_BASE_URL%"=="" set "BACKEND_BASE_URL=http://172.30.224.1:8080"

echo Starting judger in WSL from %JUDGER_WIN_PATH%
echo Using BACKEND_BASE_URL=%BACKEND_BASE_URL%
wsl.exe bash -lc "export BACKEND_BASE_URL='%BACKEND_BASE_URL%' && cd \"$(wslpath '%JUDGER_WIN_PATH%')\" && chmod +x ./run-wsl.sh && ./run-wsl.sh; exec bash"

endlocal
