@echo off
setlocal

if "%BACKEND_BASE_URL%"=="" set BACKEND_BASE_URL=http://localhost:8080
if "%JUDGE_TOKEN%"=="" set JUDGE_TOKEN=dev-judge-token
if "%JUDGER_ID_PREFIX%"=="" set JUDGER_ID_PREFIX=local-judger
if "%POLL_INTERVAL_MS%"=="" set POLL_INTERVAL_MS=2000
if "%CXX%"=="" set CXX=g++

echo Starting judger prefix %JUDGER_ID_PREFIX% against %BACKEND_BASE_URL%

sbt run

endlocal
