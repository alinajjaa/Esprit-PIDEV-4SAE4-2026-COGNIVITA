param(
  [switch]$Down
)

$ErrorActionPreference = "Stop"

if ($Down) {
  Write-Host "Arrêt SonarQube..."
  docker compose -f ..\docker-compose.sonarqube.yml down
  exit 0
}

Write-Host "Démarrage SonarQube (UI: http://localhost:9000)..."
docker compose -f ..\docker-compose.sonarqube.yml up -d

Write-Host "Astuce: login par défaut SonarQube = admin / admin (à changer au 1er login)."

