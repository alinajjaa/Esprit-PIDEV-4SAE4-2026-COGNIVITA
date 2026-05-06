<#
  Workflow dev local (sans modifier les ports des microservices) :
  1) Démarre l'infrastructure Docker locale
  2) Rappelle les ports originaux à garder dans IntelliJ
  3) Exécute un health-check
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $scriptDir "start-docker.ps1")

Write-Host ""
Write-Host "Lancer ensuite les microservices dans IntelliJ avec leurs ports ORIGINAUX :"
Write-Host "- activities-service : 8087"
Write-Host "- api-gateway       : 9090"
Write-Host "- eureka-server     : 8761"
Write-Host ""
Write-Host "Ports Docker locaux (infra seulement) :"
Write-Host "- MySQL      : 3307"
Write-Host "- Prometheus : 9091"
Write-Host "- Grafana    : 3001"

Write-Host ""
& (Join-Path $scriptDir "health-check.ps1")

