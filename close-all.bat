@echo off
echo Closing all server windows...

taskkill /F /FI "WINDOWTITLE eq FASTAPI_SERVER*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq SPRING_BOOT_SERVER*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq ANGULAR_SERVER*" >nul 2>&1

exit
