@echo off
setlocal

set "DB_HOST=127.0.0.1"
set "DB_PORT=5432"
set "DB_NAME=qiwen_online_judge"
set "DB_USER=db"
set "DB_PASSWORD=root"

echo Starting backend with:
echo   DB_HOST=%DB_HOST%
echo   DB_PORT=%DB_PORT%
echo   DB_NAME=%DB_NAME%
echo   DB_USER=%DB_USER%

sbt run

endlocal
