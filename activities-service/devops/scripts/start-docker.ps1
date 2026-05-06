<#
  Démarre uniquement l'infrastructure locale DevOps:
  - MySQL (3307)
  - Prometheus (9091)
  - Grafana (3001)
  Sans démarrer les microservices.
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = (Resolve-Path (Join-Path $scriptDir "..\docker-compose.devops.yml")).ProviderPath

Write-Host "Démarrage infra Docker locale..."
docker compose -f $composeFile up -d

Write-Host "Attente MySQL (healthcheck)..."
$max = 60
for ($i = 0; $i -lt $max; $i++) {
  $health = docker inspect --format='{{.State.Health.Status}}' activities-mysql 2>$null
  if ($health -eq "healthy") {
    Write-Host "MySQL prêt."
    Write-Host "Prometheus: http://localhost:9091"
    Write-Host "Grafana:    http://localhost:3001 (admin/admin)"
    exit 0
  }
  Start-Sleep -Seconds 2
}

Write-Error "MySQL n'est pas prêt dans le délai imparti."
exit 1

