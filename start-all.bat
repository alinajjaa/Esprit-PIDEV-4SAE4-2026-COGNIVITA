@echo off
echo Starting all servers...

start "FASTAPI_SERVER" cmd /k "cd api && python -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload"
start "SPRING_BOOT_SERVER" cmd /k "cd Backend && mvnw.cmd spring-boot:run"
start "ANGULAR_SERVER" cmd /k "cd frontend && ng serve"

echo All servers started.
exit
