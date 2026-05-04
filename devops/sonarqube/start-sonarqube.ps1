$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
  Copy-Item ".\\.env.example" ".\\.env"
  Write-Host "Created .env from .env.example. Edit SONAR_JDBC_PASSWORD before continuing." -ForegroundColor Yellow
}

docker compose pull
docker compose up -d --force-recreate

Write-Host ""
Write-Host "SonarQube UI: http://localhost:$($env:SONAR_HTTP_PORT)" -ForegroundColor Cyan
Write-Host "If this is the first boot, wait 1-3 minutes then refresh." -ForegroundColor Cyan
