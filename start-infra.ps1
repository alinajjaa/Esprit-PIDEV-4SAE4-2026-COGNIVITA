param(
  [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

Write-Host "Démarrage infrastructure uniquement (MySQL + Prometheus + Grafana + Alertmanager)..."
docker compose -f .\docker-compose.infrastructure.yml up -d

Write-Host "Attente MySQL (healthcheck)..."
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
  $status = docker inspect --format='{{.State.Health.Status}}' mysql-activities 2>$null
  if ($status -eq "healthy") {
    Write-Host "OK: MySQL est healthy."
    Write-Host "Prometheus: http://localhost:9092"
    Write-Host "Grafana:    http://localhost:3000 (admin/admin)"
    Write-Host "Alertmanager: http://localhost:9093"
    exit 0
  }
  Start-Sleep -Seconds 3
}

Write-Error "Timeout: MySQL n'est pas prêt après $TimeoutSeconds secondes."
docker compose -f .\docker-compose.infrastructure.yml ps
exit 1

