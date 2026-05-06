param(
  [switch]$Down
)

$ErrorActionPreference = "Stop"

if ($Down) {
  Write-Host "Arrêt Jenkins..."
  docker compose -f ..\docker-compose.jenkins.yml down -v --remove-orphans
  exit 0
}

Write-Host "Démarrage Jenkins (UI: http://localhost:8080)..."
docker compose -f ..\docker-compose.jenkins.yml up -d --build

Write-Host "Plugins installés via jenkins/plugins.txt. Si c'est le 1er démarrage, attends 1-2 minutes."

