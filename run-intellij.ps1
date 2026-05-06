param(
  # Chemin IntelliJ (optionnel). Exemple:
  # "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3\bin\idea64.exe"
  [string]$IdeaExe = "",
  [string]$ProjectPath = (Get-Location).Path
)

$ErrorActionPreference = "Stop"

# Charge .env.local si présent
$envFile = Join-Path $PSScriptRoot ".env.local"
if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $kv = $_ -split '=', 2
    if ($kv.Length -eq 2) {
      $name = $kv[0].Trim()
      $value = $kv[1].Trim()
      [Environment]::SetEnvironmentVariable($name, $value)
    }
  }
}

$activitiesPort = $env:ACTIVITIES_PORT
if ([string]::IsNullOrWhiteSpace($activitiesPort)) { $activitiesPort = "8088" }
$gatewayPort = $env:GATEWAY_PORT
if ([string]::IsNullOrWhiteSpace($gatewayPort)) { $gatewayPort = "9091" }

Write-Host "Ports IntelliJ locaux:"
Write-Host "- activities-service: $activitiesPort"
Write-Host "- api-gateway:       $gatewayPort"

Write-Host "Démarrage infra Docker..."
& "$PSScriptRoot\start-infra.ps1"

Write-Host ""
Write-Host "Configuration IntelliJ (à mettre dans Run/Debug):"
Write-Host "activities-service VM options:"
Write-Host "  -Dserver.port=$activitiesPort"
Write-Host "  -Dspring.datasource.url=jdbc:mysql://localhost:3307/cognivita_activities?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
Write-Host "  -Dspring.datasource.username=cognivita"
Write-Host "  -Dspring.datasource.password=password"
Write-Host ""
Write-Host "api-gateway VM options:"
Write-Host "  -Dserver.port=$gatewayPort"
Write-Host ""

# Lancement IntelliJ (optionnel)
if (-not [string]::IsNullOrWhiteSpace($IdeaExe)) {
  if (-not (Test-Path $IdeaExe)) {
    throw "idea64.exe introuvable: $IdeaExe"
  }
  Write-Host "Ouverture IntelliJ..."
  Start-Process -FilePath $IdeaExe -ArgumentList @("$ProjectPath")
} else {
  Write-Host "INFO: IntelliJ non lancé automatiquement (fournis -IdeaExe si tu veux)."
}

