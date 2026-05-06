param(
  [int]$Port = 8087
)

$ErrorActionPreference = "Stop"

Write-Host "Délégation vers le test.ps1 racine..."
& "$PSScriptRoot\..\test.ps1" -Port $Port

