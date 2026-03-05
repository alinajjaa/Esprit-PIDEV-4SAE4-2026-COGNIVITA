@echo off
setlocal

REM Determine project root based on this script location
set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

echo.
echo ==========================================
echo  Starting COGNIVITA backend from JARs
echo  (requires successful build-backend.bat)
echo ==========================================
echo.

cd /d "%BASE_DIR%Backend"

REM Check one JAR to verify build ran
if not exist "admin-service\target\admin-service-0.0.1-SNAPSHOT.jar" (
  echo JARs not found. Please run build-backend.bat first.
  echo.
  pause
  endlocal
  exit /b 1
)

echo Starting Eureka server (port 8080)...
start "eureka-server" cmd /k "cd /d \"%CD%\eureka-server\" && java -jar target\eureka-server-0.0.1-SNAPSHOT.jar"

echo Starting User service (port 8082)...
start "user-service" cmd /k "cd /d \"%CD%\user-service\" && java -jar target\user-service-0.0.1-SNAPSHOT.jar"

echo Starting MMSE service (port 8085)...
start "mmse-service" cmd /k "cd /d \"%CD%\mmse-service\" && java -jar target\mmse-service-0.0.1-SNAPSHOT.jar"

echo Starting Medical service (port 8083)...
start "medical-service" cmd /k "cd /d \"%CD%\medical-service\" && java -jar target\medical-service-0.0.1-SNAPSHOT.jar"

echo Starting CNN service (port 8084)...
start "cnn-service" cmd /k "cd /d \"%CD%\cnn-service\" && java -jar target\cnn-service-0.0.1-SNAPSHOT.jar"

echo Starting Admin service (port 8081)...
start "admin-service" cmd /k "cd /d \"%CD%\admin-service\" && java -jar target\admin-service-0.0.1-SNAPSHOT.jar"

echo.
echo All backend services are starting in their own windows.
echo Make sure:
echo   1) Java (JDK 17+) is installed and on PATH (java -version)
echo   2) MySQL is running on localhost:3306 with correct credentials
echo.
pause

endlocal

