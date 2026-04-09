@echo off
setlocal

echo Starting frontend on http://127.0.0.1:5173

npm run dev -- --host 127.0.0.1 --port 5173

endlocal
