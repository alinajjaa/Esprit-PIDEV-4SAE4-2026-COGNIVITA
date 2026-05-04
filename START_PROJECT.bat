@echo off
chcp 65001 >nul
SET ROOT=%~dp0
SET ROOT=%ROOT:~0,-1%

echo ============================================================
echo   COGNIVITA - Alzheimer Platform Startup
echo ============================================================
echo.
echo [1/9] Verifying MySQL is running on port 3306...
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] MySQL is NOT running! Please start XAMPP MySQL first.
    pause
    exit /b 1
)
echo [OK] MySQL is running.
echo.

echo [2/9] Starting Eureka Server (port 8761)...
start "EUREKA-SERVER" cmd /k "title EUREKA-SERVER && cd /d "%ROOT%\eureka-server" && mvn spring-boot:run -q"
echo [OK] Eureka window launched.
echo.

echo Waiting 20 seconds for Eureka to initialize...
timeout /t 20 /nobreak >nul

echo [3/9] Starting API Gateway (port 9090)...
start "API-GATEWAY" cmd /k "title API-GATEWAY && cd /d "%ROOT%\api-gateway" && mvn spring-boot:run -q"
echo [OK] API Gateway window launched.
echo.

echo [4/9] Starting User Service (port 8084)...
start "USER-SERVICE" cmd /k "title USER-SERVICE && cd /d "%ROOT%\user-service" && mvn spring-boot:run -q"
echo [OK] User Service window launched.
echo.

echo [5/9] Starting MMSE Service (port 8085)...
start "MMSE-SERVICE" cmd /k "title MMSE-SERVICE && cd /d "%ROOT%\mmse-service" && mvn spring-boot:run -q"
echo [OK] MMSE Service window launched.
echo.

echo [6/9] Starting Medical Records Service (port 8081)...
start "MEDICAL-RECORDS" cmd /k "title MEDICAL-RECORDS && cd /d "%ROOT%\medical-records-service" && mvn spring-boot:run -q"
echo [OK] Medical Records window launched.
echo.

echo [7/9] Starting Family Tree Service (port 8083)...
start "FAMILY-TREE" cmd /k "title FAMILY-TREE && cd /d "%ROOT%\family-tree-service" && mvn spring-boot:run -q"
echo [OK] Family Tree window launched.
echo.

echo [8/9] Starting Activities Service (port 8087)...
start "ACTIVITIES-SERVICE" cmd /k "title ACTIVITIES-SERVICE && cd /d "%ROOT%\activities-service" && mvn spring-boot:run -q"
echo [OK] Activities Service window launched.
echo.

echo [8b] Starting Health Prevention Service (port 8082)...
start "HEALTH-PREVENTION" cmd /k "title HEALTH-PREVENTION && cd /d "%ROOT%\health-prevention-service" && mvn spring-boot:run -q"
echo [OK] Health Prevention window launched.
echo.

echo [8c] Starting Medication Adherence Service (port 8086)...
start "MEDICATION-ADHERENCE" cmd /k "title MEDICATION-ADHERENCE && cd /d "%ROOT%\medication-adherence-service" && mvn spring-boot:run -q"
echo [OK] Medication Adherence window launched.
echo.

echo [8d] Starting Notification Service (port 8088)...
start "NOTIFICATION-SERVICE" cmd /k "title NOTIFICATION-SERVICE && cd /d "%ROOT%\notification-service" && mvn spring-boot:run -q"
echo [OK] Notification Service window launched.
echo.

echo [8e] Starting Rendez-vous Service / pi (port 8091)...
start "RDV-SERVICE" cmd /k "title RDV-SERVICE && cd /d "%ROOT%\pi\pi" && mvn spring-boot:run -q"
echo [OK] Rendez-vous Service window launched.
echo.

echo [8f] Starting Plan Suivi Service (port 8092)...
start "PLAN-SUIVI" cmd /k "title PLAN-SUIVI && cd /d "%ROOT%\planSuivi\planSuivi" && mvn spring-boot:run -q"
echo [OK] Plan Suivi window launched.
echo.

echo [8g] Starting Backend monolith (port 8080)...
start "BACKEND" cmd /k "title BACKEND && cd /d "%ROOT%\Backend" && mvnw.cmd spring-boot:run"
echo [OK] Backend window launched.
echo.

echo Waiting 15 seconds before launching frontend...
timeout /t 15 /nobreak >nul

echo [9/9] Starting Angular Frontend (port 4200)...
start "ANGULAR-FRONTEND" cmd /k "title ANGULAR-FRONTEND && cd /d "%ROOT%\frontend" && npm start"
echo [OK] Angular Frontend window launched.
echo.

echo ============================================================
echo   All services are starting in separate windows!
echo ============================================================
echo.
echo   Service URLs:
echo   - Eureka Dashboard  : http://localhost:8761
echo   - API Gateway       : http://localhost:9090
echo   - Angular Frontend  : http://localhost:4200
echo   - Backend (mono)    : http://localhost:8080
echo   - User Service      : http://localhost:8084
echo   - MMSE Service      : http://localhost:8085
echo   - Medical Records   : http://localhost:8081
echo   - Family Tree       : http://localhost:8083
echo   - Activities        : http://localhost:8087
echo   - Health Prevention : http://localhost:8082
echo   - Med. Adherence    : http://localhost:8086
echo   - Notification      : http://localhost:8088
echo   - Rendez-vous       : http://localhost:8091
echo   - Plan Suivi        : http://localhost:8092
echo.
echo   Swagger UIs are accessible at /swagger-ui.html per service.
echo.
pause
