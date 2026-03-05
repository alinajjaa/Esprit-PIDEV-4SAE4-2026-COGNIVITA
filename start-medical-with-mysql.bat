@echo off
REM Run Medical Service with MySQL (alzheimer_db). Your MySQL root has NO password (phpMyAdmin User accounts show "Non").
set MYSQL_PASSWORD=
cd /d "%~dp0Backend"
echo Starting Medical Service (8083) with MySQL (no password)...
call mvnw.cmd -pl medical-service spring-boot:run
pause
