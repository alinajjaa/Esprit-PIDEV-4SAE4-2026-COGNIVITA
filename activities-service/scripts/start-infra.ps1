<#
  Démarre uniquement l'infrastructure locale (mysql, prometheus, grafana) via docker compose fichier local.
  Conçu pour Docker Desktop sur Windows.
#>

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = Join-Path $scriptDir "..\docker-compose.activities.yml"
$composeFile = (Resolve-Path $composeFile).ProviderPath

Write-Host "Démarrage des services d'infrastructure via $composeFile ..."
docker compose -f $composeFile up -d mysql-activities prometheus-activities grafana-activities

# Attendre que MySQL soit prêt sur le port localhost:3307
$maxRetries = 30
$retry = 0
Write-Host "Attente de MySQL sur localhost:3307 ..."
while ($retry -lt $maxRetries) {
    $res = Test-NetConnection -ComputerName 'localhost' -Port 3307 -WarningAction SilentlyContinue
    if ($res.TcpTestSucceeded) {
        Write-Host "MySQL accessible (localhost:3307)."
        break
    }
    Start-Sleep -Seconds 2
    $retry++
    Write-Host -NoNewline "."
}
if ($retry -ge $maxRetries) {
    Write-Error "MySQL n'a pas démarré dans le délai imparti."
    exit 1
}

Write-Host "`nServices démarrés :"
Write-Host " - MySQL  : http://localhost:3307 (base cognivita_activities)"
Write-Host " - Prometheus : http://localhost:9091"
Write-Host " - Grafana    : http://localhost:3001"
Write-Host "`nVous pouvez maintenant lancer l'application depuis IntelliJ (profil local) ou construire l'image via scripts\build.ps1"

