# Vérifie les endpoints REST du service activities-service.
# Variables : SERVICE_BASE_URL (défaut http://host.docker.internal:8087)
param()

$Base = if ($env:SERVICE_BASE_URL) { $env:SERVICE_BASE_URL } else { 'http://host.docker.internal:8087' }
$endpoints = @('/api/activities', '/api/journal', '/api/activities/statistics/global')
$allOk = $true

foreach ($e in $endpoints) {
    $url = "$Base$e"
    Write-Host "Checking $url ..."
    try {
        $resp = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 10 -ErrorAction Stop
        if ($resp -eq $null) {
            Write-Warning "Réponse vide pour $url"
            $allOk = $false
        } else {
            Write-Host "OK: $url returned data"
        }
    } catch {
        Write-Error "Échec pour $url : $_"
        $allOk = $false
    }
}

if ($allOk) { Write-Host "Tous les endpoints sont OK"; exit 0 } else { Write-Error "Un ou plusieurs endpoints ont échoué"; exit 2 }

