<#
  Arrête la stack DevOps locale sans toucher aux autres stacks.
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = (Resolve-Path (Join-Path $scriptDir "..\docker-compose.devops.yml")).ProviderPath

Write-Host "Arrêt infra Docker locale..."
docker compose -f $composeFile down --remove-orphans

