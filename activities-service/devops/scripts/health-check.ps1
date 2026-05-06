<#
  Vérifie les services locaux nécessaires au dev.
#>

$ErrorActionPreference = "Stop"
$errors = @()

function Test-Http([string]$url) {
  try {
    $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 8
    return $resp.StatusCode
  } catch {
    return $null
  }
}

function Test-Port([int]$port) {
  $r = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
  return $r.TcpTestSucceeded
}

Write-Host "MySQL localhost:3307"
if (-not (Test-Port 3307)) { $errors += "MySQL KO (3307)" }

Write-Host "Prometheus http://localhost:9091"
$code = Test-Http "http://localhost:9091/-/healthy"
if ($code -ne 200) { $errors += "Prometheus KO (9091)" }

Write-Host "Grafana http://localhost:3001/api/health"
$code = Test-Http "http://localhost:3001/api/health"
if ($code -ne 200) { $errors += "Grafana KO (3001)" }

Write-Host "Activities API (IntelliJ) http://localhost:8087/api/journal"
$code = Test-Http "http://localhost:8087/api/journal"
if ($null -eq $code) { $errors += "Activities API KO (8087)" }

Write-Host "API Gateway (IntelliJ) http://localhost:9090/actuator/health"
$code = Test-Http "http://localhost:9090/actuator/health"
if ($null -eq $code) { $errors += "API Gateway KO (9090)" }

if ($errors.Count -gt 0) {
  Write-Host "`nErreurs:"
  $errors | ForEach-Object { Write-Host "- $_" }
  exit 1
}

Write-Host "`nTous les checks sont OK."

