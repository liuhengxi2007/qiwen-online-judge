@echo off
setlocal

if "%DB_HOST%"=="" set "DB_HOST=127.0.0.1"
if "%DB_PORT%"=="" set "DB_PORT=5432"
if "%DB_NAME%"=="" set "DB_NAME=qiwen_online_judge"
if "%DB_USER%"=="" set "DB_USER=db"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"

if "%MINIO_ENDPOINT%"=="" set "MINIO_ENDPOINT=http://127.0.0.1:9000"
if "%MINIO_ACCESS_KEY%"=="" set "MINIO_ACCESS_KEY=minioadmin"
if "%MINIO_SECRET_KEY%"=="" set "MINIO_SECRET_KEY=minioadmin"
if "%MINIO_BUCKET%"=="" set "MINIO_BUCKET=qiwen-online-judge"
if "%MINIO_SECURE%"=="" set "MINIO_SECURE=false"

if "%JUDGE_SHARED_TOKEN%"=="" set "JUDGE_SHARED_TOKEN=dev-judge-token"

echo Starting backend with:
echo   DB_HOST=%DB_HOST%
echo   DB_PORT=%DB_PORT%
echo   DB_NAME=%DB_NAME%
echo   DB_USER=%DB_USER%
echo   MINIO_ENDPOINT=%MINIO_ENDPOINT%
echo   MINIO_BUCKET=%MINIO_BUCKET%
echo   MINIO_SECURE=%MINIO_SECURE%

sbt run

endlocal
