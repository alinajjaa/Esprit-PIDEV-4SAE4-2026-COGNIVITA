@echo off
REM Medical Service with H2 (no MySQL). Always starts; records are empty. Use this when start-medical-with-mysql.bat fails.
cd /d "%~dp0Backend"
echo Starting Medical Service (8083) with H2...
call mvnw.cmd -pl medical-service spring-boot:run -Dspring-boot.run.profiles=dev
pause
