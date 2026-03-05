@echo off
setlocal

REM Determine project root based on this script location
set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

echo.
echo ==========================================
echo  Building COGNIVITA backend (all modules)
echo ==========================================
echo.

cd /d "%BASE_DIR%Backend"

echo Running Maven wrapper: clean package -DskipTests
echo This may take a few minutes on first run...
echo.

call .\mvnw clean package -DskipTests

if errorlevel 1 (
  echo.
  echo **************************************
  echo  BUILD FAILED - see errors above
  echo **************************************
  echo.
  pause
  endlocal
  exit /b 1
)

echo.
echo **************************************
echo  BUILD SUCCESS
echo  JARs generated under:
echo    Backend\*\target\*-0.0.1-SNAPSHOT.jar
echo **************************************
echo.
pause

endlocal

