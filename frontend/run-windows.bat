@echo off
setlocal

echo Starting frontend on http://127.0.0.1:5173

call npm install
if errorlevel 1 (
    echo npm install failed.
    pause
    exit /b 1
)

call npm run dev -- --host 127.0.0.1 --port 5173
if errorlevel 1 (
    echo npm run dev failed.
    pause
    exit /b 1
)

endlocal
