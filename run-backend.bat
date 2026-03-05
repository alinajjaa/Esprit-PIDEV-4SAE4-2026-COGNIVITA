@echo off
setlocal

REM Determine project root based on this script location
set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

echo.
echo ==========================================
echo  Starting COGNIVITA backend microservices
echo ==========================================
echo.

REM Go to Backend folder where mvnw lives
cd /d "%BASE_DIR%Backend"

echo Starting Eureka server (port 8080)...
start "eureka-server" cmd /k ".\mvnw -pl eureka-server spring-boot:run"

echo Starting User service (port 8082)...
start "user-service" cmd /k ".\mvnw -pl user-service spring-boot:run"

echo Starting MMSE service (port 8085)...
start "mmse-service" cmd /k ".\mvnw -pl mmse-service spring-boot:run"

echo Starting Medical service (port 8083)...
start "medical-service" cmd /k ".\mvnw -pl medical-service spring-boot:run"

echo Starting CNN service (port 8084)...
start "cnn-service" cmd /k ".\mvnw -pl cnn-service spring-boot:run"

echo Starting Admin service (port 8081)...
start "admin-service" cmd /k ".\mvnw -pl admin-service spring-boot:run"

echo.
echo All backend services are starting in their own windows.
echo Make sure MySQL is running on localhost:3306 and credentials in
echo Backend\user-service and Backend\mmse-service and Backend\medical-service
echo application.properties are correct.
echo.
pause

endlocal

@echo off
cd /d C:\Alzheimer-Detection-System\Backend
echo Starting Alzheimer Detection System Backend...
echo.
echo Backend will be available at: http://localhost:8080
echo API Documentation:
echo   - GET  /api/users           - Get all users
echo   - POST /api/users           - Create new user
echo   - GET  /api/users/{id}      - Get user by ID
echo   - PUT  /api/users/{id}      - Update user
echo   - DELETE /api/users/{id}    - Delete user
echo.
echo   - GET  /api/mmse-tests      - Get all MMSE tests
echo   - POST /api/mmse-tests      - Create MMSE test
echo   - GET  /api/admin/dashboard - Get admin dashboard
echo   - GET  /api/admin/stats     - Get admin statistics
echo.
echo MySQL Database: alzheimer_db
echo phpMyAdmin URL: http://localhost/phpmyadmin
echo.
call mvnw spring-boot:run
pause
