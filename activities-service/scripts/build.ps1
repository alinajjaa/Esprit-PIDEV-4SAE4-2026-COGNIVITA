<#
  Construit l'image Docker activities-service localement
#>
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projRoot = Resolve-Path (Join-Path $scriptDir "..")

Write-Host "Construction de l'image activities-service:local ..."
Push-Location $projRoot
docker build -t activities-service:local .
if ($LASTEXITCODE -ne 0) {
    Write-Error "Erreur lors de la construction de l'image."
    Pop-Location
    exit 1
}
Write-Host "Image construite: activities-service:local"
Pop-Location

