param(
  [string]$ImageName = "cognivita/activities-service:local",
  # Exemple: tu peux builder une autre target/tag si besoin
  [string]$ContextPath = "."
)

$ErrorActionPreference = "Stop"

Write-Host "Build de l'image Docker: $ImageName"
docker build -t $ImageName $ContextPath

Write-Host "OK: image construite. Utilise start.ps1 pour lancer l'environnement complet (MySQL + service)."
