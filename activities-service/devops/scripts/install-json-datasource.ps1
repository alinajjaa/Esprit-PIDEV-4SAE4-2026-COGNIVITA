# Installe le plugin JSON API (simpod-json-datasource) dans le conteneur Grafana et redémarre le conteneur.
# Variables (modifiable via variables d'environnement):
# $env:GRAFANA_CONTAINER (défaut: grafana)
# $env:PLUGIN_ID (défaut: simpod-json-datasource)
# $env:GRAFANA_URL (défaut: http://localhost:3001)

param()

$GrafanaContainer = if ($env:GRAFANA_CONTAINER) { $env:GRAFANA_CONTAINER } else { 'grafana' }
$PluginId = if ($env:PLUGIN_ID) { $env:PLUGIN_ID } else { 'simpod-json-datasource' }
$GrafanaUrl = if ($env:GRAFANA_URL) { $env:GRAFANA_URL } else { 'http://localhost:3001' }

Write-Host "[*] Installer le plugin '$PluginId' dans le conteneur '$GrafanaContainer'..."

# Vérifier que le conteneur existe
$ps = docker ps --format "{{.Names}}" | Select-String -Pattern "^$GrafanaContainer$"
if (-not $ps) {
    Write-Error "Le conteneur '$GrafanaContainer' ne semble pas en cours d'exécution. Veuillez vérifier le nom du conteneur Grafana (GRAFANA_CONTAINER)."
    exit 2
}

# Installer le plugin
try {
    Write-Host "[+] Exécution: docker exec -u root $GrafanaContainer grafana-cli plugins install $PluginId"
    docker exec -u root $GrafanaContainer grafana-cli plugins install $PluginId | Write-Host
} catch {
    Write-Warning "L'installation via grafana-cli a retourné une erreur (peut-être déjà installé). Continuation..."
}

# Redémarrer le conteneur
Write-Host "[+] Redémarrage du conteneur Grafana..."
docker restart $GrafanaContainer | Write-Host

# Attendre que Grafana soit disponible
$maxTries = 60
$try = 0
while ($try -lt $maxTries) {
    try {
        $resp = Invoke-WebRequest -Uri $GrafanaUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400) {
            Write-Host "[+] Grafana répond sur $GrafanaUrl"
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
        $try++
    }
}

Write-Error "Grafana n'est pas disponible après l'installation du plugin. Veuillez vérifier le conteneur et les logs."
exit 3

