# Script complet pour installer plugin JSON API, créer la datasource et importer le dashboard.
# Variables d'environnement: GRAFANA_URL, GRAFANA_ADMIN_USER, GRAFANA_ADMIN_PASSWORD, DATASOURCE_NAME, DATASOURCE_URL

param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$InstallScript = Join-Path $ScriptDir 'install-json-datasource.ps1'
$SetupDS = Join-Path $ScriptDir 'setup-json-datasource.ps1'
$DashboardFile = Join-Path $ScriptDir '..\dashboards\cognitive-activities-dashboard.json'

Write-Host "[*] Lancement de l'installation graphique (plugin + datasource + dashboard)"

# 1) Installer le plugin
Write-Host "[1/3] Installer le plugin JSON API..."
& $InstallScript
if ($LASTEXITCODE -ne 0) { Write-Error "Echec de l'installation du plugin"; exit $LASTEXITCODE }

# 2) Créer la datasource
Write-Host "[2/3] Création de la datasource JSON API..."
& $SetupDS
if ($LASTEXITCODE -ne 0) { Write-Error "Echec de la création de la datasource"; exit $LASTEXITCODE }

# 3) Importer le dashboard
Write-Host "[3/3] Import du dashboard Grafana..."
$GrafanaUrl = if ($env:GRAFANA_URL) { $env:GRAFANA_URL } else { 'http://localhost:3001' }
$GrafanaUser = if ($env:GRAFANA_ADMIN_USER) { $env:GRAFANA_ADMIN_USER } else { 'admin' }
$GrafanaPass = if ($env:GRAFANA_ADMIN_PASSWORD) { $env:GRAFANA_ADMIN_PASSWORD } else { '' }
$DatasourceName = if ($env:DATASOURCE_NAME) { $env:DATASOURCE_NAME } else { 'Activities JSON API' }

# Lire le dashboard JSON et remplacer le placeholder __DATASOURCE__ par le nom réel
if (-not (Test-Path $DashboardFile)) { Write-Error "Dashboard non trouvé: $DashboardFile"; exit 4 }
$dashraw = Get-Content $DashboardFile -Raw
$dashraw = $dashraw -replace '__DATASOURCE__', $DatasourceName
$body = @{ dashboard = (ConvertFrom-Json $dashraw); overwrite = $true } | ConvertTo-Json -Depth 100

# Auth header
$auth = "{0}:{1}" -f $GrafanaUser,$GrafanaPass
$base64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($auth))
$headers = @{ Authorization = "Basic $base64"; Accept = 'application/json' }

$importApi = "$GrafanaUrl/api/dashboards/db"
try {
    $resp = Invoke-RestMethod -Method Post -Uri $importApi -Headers $headers -Body $body -ContentType 'application/json' -ErrorAction Stop
    Write-Host "[+] Dashboard importé: uid=$($resp.uid) url=$($resp.url)"
    exit 0
} catch {
    Write-Error "Erreur lors de l'import du dashboard: $_"
    exit 5
}

