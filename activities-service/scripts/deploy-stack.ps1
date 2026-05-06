param(
  [switch]$WithSonar,
  [switch]$WithJenkins
)

$ErrorActionPreference = "Stop"

Write-Host "Déploiement stack principale (MySQL + activities-service + Prometheus + Grafana)..."
docker compose up -d --build

if ($WithSonar) {
  Write-Host "Déploiement SonarQube..."
  docker compose -f ..\docker-compose.sonarqube.yml up -d
}

if ($WithJenkins) {
  Write-Host "Déploiement Jenkins..."
  docker compose -f ..\docker-compose.jenkins.yml up -d --build
}

Write-Host "URLs:"
Write-Host "- API:      http://localhost:8087/api"
Write-Host "- Prometheus: http://localhost:9090"
Write-Host "- Grafana:    http://localhost:3000 (admin/admin)"
if ($WithSonar) { Write-Host "- SonarQube:  http://localhost:9000" }
if ($WithJenkins) { Write-Host "- Jenkins:    http://localhost:8080" }

