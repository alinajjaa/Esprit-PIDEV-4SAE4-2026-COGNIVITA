# Crée une datasource JSON API dans Grafana via l'API HTTP.
# Variables attendues en environnement :
# GRAFANA_URL (ex: http://localhost:3001)
# GRAFANA_ADMIN_USER (ex: admin)
# GRAFANA_ADMIN_PASSWORD
# DATASOURCE_NAME (ex: Activities JSON API)
# DATASOURCE_URL (ex: http://host.docker.internal:8087)

param()

$GrafanaUrl = if ($env:GRAFANA_URL) { $env:GRAFANA_URL } else { 'http://localhost:3001' }
$GrafanaUser = if ($env:GRAFANA_ADMIN_USER) { $env:GRAFANA_ADMIN_USER } else { 'admin' }
$GrafanaPass = if ($env:GRAFANA_ADMIN_PASSWORD) { $env:GRAFANA_ADMIN_PASSWORD } else { '' }
$DatasourceName = if ($env:DATASOURCE_NAME) { $env:DATASOURCE_NAME } else { 'Activities JSON API' }
$DatasourceUrl = if ($env:DATASOURCE_URL) { $env:DATASOURCE_URL } else { 'http://host.docker.internal:8087' }

if (-not $GrafanaPass) {
    Write-Warning "GRAFANA_ADMIN_PASSWORD non défini. L'authentification peut échouer."
}

$authHeader = "{0}:{1}" -f $GrafanaUser,$GrafanaPass
$bytes = [System.Text.Encoding]::UTF8.GetBytes($authHeader)
$base64 = [System.Convert]::ToBase64String($bytes)
$headers = @{ Authorization = "Basic $base64"; Accept = 'application/json' }

# Attendre Grafana
$maxTries = 60; $try = 0
while ($try -lt $maxTries) {
    try {
        $r = Invoke-WebRequest -Uri $GrafanaUrl -Headers $headers -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        break
    } catch {
        Start-Sleep -Seconds 2
        $try++
    }
}
if ($try -ge $maxTries) { Write-Error "Grafana indisponible à $GrafanaUrl"; exit 3 }

# Construire le payload pour la datasource JSON API
$payload = @{
    name = $DatasourceName
    type = 'simpod-json-datasource'
    access = 'proxy'
    url = $DatasourceUrl
    basicAuth = $false
    jsonData = @{ }
} | ConvertTo-Json -Depth 5

$dsApi = "$GrafanaUrl/api/datasources"
Write-Host "[+] Création de la datasource '$DatasourceName' via $dsApi"

try {
    $resp = Invoke-RestMethod -Method Post -Uri $dsApi -Headers $headers -Body $payload -ContentType 'application/json' -ErrorAction Stop
    Write-Host "[+] Datasource créée: id=$($resp.id) name=$($resp.name)"
    exit 0
} catch [System.Net.WebException] {
    $web = $_.Exception.Response
    if ($web -ne $null) {
        $sr = New-Object System.IO.StreamReader($web.GetResponseStream())
        $body = $sr.ReadToEnd() | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($body -and $body.message -match 'already exists') {
            Write-Host "[i] Datasource existe déjà. On récupère sa définition."
            $get = Invoke-RestMethod -Method Get -Uri "$GrafanaUrl/api/datasources/name/$DatasourceName" -Headers $headers -ErrorAction Stop
            Write-Host "[+] Datasource trouvée: id=$($get.id) name=$($get.name)"
            exit 0
        }
        Write-Error "Erreur lors de la création de la datasource: $body"
        exit 4
    } else {
        Write-Error "Erreur inconnue lors de la création de la datasource: $_"
        exit 5
    }
} catch {
    Write-Error "Erreur lors de la création de la datasource: $_"
    exit 6
}

