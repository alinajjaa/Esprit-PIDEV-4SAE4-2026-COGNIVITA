param(
  [switch]$RemoveVolumes
)

$ErrorActionPreference = "Stop"

Write-Host "Arrêt des services docker compose..."
if ($RemoveVolumes) {
  # ATTENTION: supprime aussi les données MySQL persistées
  docker compose down -v --remove-orphans
} else {
  docker compose down --remove-orphans
}

