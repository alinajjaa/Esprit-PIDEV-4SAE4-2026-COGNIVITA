@echo off
echo Stopping processes on backend ports 8080-8085...
for %%p in (8080 8081 8082 8083 8084 8085) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%%p ^| findstr LISTENING') do (
    taskkill /F /PID %%a 2>nul && echo Killed PID %%a on port %%p
  )
)
echo Done. You can now run start-all.bat.
pause
