param(
  [int]$Port = 8087,
  [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

Write-Host "Nettoyage d'éventuels anciens conteneurs (docker run / anciens essais)..."
docker rm -f activities-service 2>$null | Out-Null
docker rm -f mysql-activities 2>$null | Out-Null

Write-Host "Démarrage via docker compose (build inclus)..."
docker compose up -d --build

Write-Host "Attente que le service soit prêt (health)..."
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

while ((Get-Date) -lt $deadline) {
  try {
    # Avec context-path=/api, l'actuator health est sous /api/actuator/health
    $resp = Invoke-RestMethod -Method Get -Uri "http://localhost:$Port/api/actuator/health" -TimeoutSec 3
    if ($resp.status -eq "UP") {
      Write-Host "OK: activities-service est UP."
      exit 0
    }
  } catch {
    # pas prêt encore
  }

  Start-Sleep -Seconds 3
}

Write-Error "Timeout: le service n'est pas prêt après $TimeoutSeconds secondes."
docker compose ps
exit 1

