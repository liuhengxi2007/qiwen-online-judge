@echo off
setlocal

for %%I in ("%~dp0.") do set "JUDGER_WIN_PATH=%%~fI"
set "JUDGER_WIN_PATH=%JUDGER_WIN_PATH:\=/%"

echo Starting judger in WSL from %JUDGER_WIN_PATH%
if not "%BACKEND_BASE_URL%"=="" echo Using BACKEND_BASE_URL=%BACKEND_BASE_URL%
wsl.exe bash -lc "if [ -n '%BACKEND_BASE_URL%' ]; then export BACKEND_BASE_URL='%BACKEND_BASE_URL%'; fi && cd \"$(wslpath '%JUDGER_WIN_PATH%')\" && sed -i 's/\r$//' ./run-wsl.sh && chmod +x ./run-wsl.sh && ./run-wsl.sh; exec bash"

endlocal
