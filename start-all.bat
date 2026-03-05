@echo off
setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"

echo ============================================
echo   Stopping any process on ports 8080-8085
echo ============================================
for %%p in (8080 8081 8082 8083 8084 8085) do (
  for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr :%%p ^| findstr LISTENING') do (
    taskkill /F /PID %%a 2>nul && echo   Freed port %%p (PID %%a)
  )
)
timeout /t 2 /nobreak >nul
echo.

echo ============================================
echo   Checking MySQL (port 3306) and port 8083
echo ============================================
netstat -ano 2>nul | findstr :3306 | findstr LISTENING >nul && (
  echo   [OK] MySQL or another service is listening on port 3306.
) || (
  echo   [!!] Nothing listening on port 3306. Start MySQL before Medical Service ^(e.g. XAMPP/WAMP/MAMP or Windows Service^).
)
netstat -ano 2>nul | findstr :8083 | findstr LISTENING >nul && (
  echo   [!!] Port 8083 still in use. Close the app using it or run stop-backend-ports.bat.
) || (
  echo   [OK] Port 8083 is free for Medical Service.
)
echo.

echo ============================================
echo   Starting Eureka first (port 8080)
echo ============================================
start "Eureka (8080)" cmd /k "cd /d "%ROOT%Backend" && mvnw.cmd -pl eureka-server spring-boot:run"
echo Waiting 20 seconds for Eureka to be ready...
timeout /t 20 /nobreak
echo.

echo ============================================
echo   Starting other backend services
echo ============================================
REM User & MMSE: dev profile (H2). Medical: MySQL so new records save to alzheimer_db and show in phpMyAdmin (root has no password).
start "User Service (8082)" cmd /k "cd /d "%ROOT%Backend" && mvnw.cmd -pl user-service spring-boot:run -Dspring-boot.run.profiles=dev"
start "MMSE Service (8085)" cmd /k "cd /d "%ROOT%Backend" && mvnw.cmd -pl mmse-service spring-boot:run -Dspring-boot.run.profiles=dev"
start "Medical Service (8083)" cmd /k "set MYSQL_PASSWORD= && cd /d "%ROOT%Backend" && mvnw.cmd -pl medical-service spring-boot:run"
start "CNN Service (8084)" cmd /k "cd /d "%ROOT%Backend" && mvnw.cmd -pl cnn-service spring-boot:run"
start "Admin Service (8081)" cmd /k "cd /d "%ROOT%Backend" && mvnw.cmd -pl admin-service spring-boot:run"

if exist "api\main.py" (
  start "FastAPI Server (8000)" cmd /k "cd /d "%ROOT%api" && uvicorn main:app --host 127.0.0.1 --port 8000 --reload"
)

start "Angular Frontend (4200)" cmd /k "cd /d "%ROOT%frontend" && ng serve"

echo.
echo All servers launched. Wait until each window shows "Started ...Application".
echo Then open http://localhost:4200
pause
endlocal
