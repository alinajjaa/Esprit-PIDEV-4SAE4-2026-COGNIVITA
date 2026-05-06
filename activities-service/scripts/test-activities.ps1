<#
  Teste l'accessibilité des services locaux :
  - MySQL (port 3307) via TCP
  - Prometheus (9091) via HTTP /
  - Grafana (3001) via HTTP /
  - API activities (8088) via /api/journal (GET)
#>

$errors = @()

Function Test-TcpPort($host, $port) {
    $r = Test-NetConnection -ComputerName $host -Port $port -WarningAction SilentlyContinue
    return $r.TcpTestSucceeded
}

Function Test-Http($url) {
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        return @{ OK = $true; Status = $resp.StatusCode }
    } catch {
        return @{ OK = $false; Error = $_.Exception.Message }
    }
}

Write-Host "Test MySQL (localhost:3307)..."
if (-not (Test-TcpPort 'localhost' 3307)) {
    $errors += "MySQL (localhost:3307) inaccessible."
    Write-Host "  -> KO"
} else { Write-Host "  -> OK" }

Write-Host "Test Prometheus (http://localhost:9091)..."
$r = Test-Http 'http://localhost:9091/'
if (-not $r.OK) { $errors += "Prometheus: $($r.Error)"; Write-Host "  -> KO" } else { Write-Host "  -> OK ($($r.Status))" }

Write-Host "Test Grafana (http://localhost:3001)..."
$r = Test-Http 'http://localhost:3001/'
if (-not $r.OK) { $errors += "Grafana: $($r.Error)"; Write-Host "  -> KO" } else { Write-Host "  -> OK ($($r.Status))" }

Write-Host "Test API activities (http://localhost:8088/api/journal)..."
$r = Test-Http 'http://localhost:8088/api/journal'
if (-not $r.OK) { $errors += "API: $($r.Error)"; Write-Host "  -> KO" } else {
    Write-Host "  -> OK ($($r.Status))"
}

if ($errors.Count -gt 0) {
    Write-Host "`nTests échoués:"
    $errors | ForEach-Object { Write-Host " - $_" }
    exit 1
} else {
    Write-Host "`nTous les tests sont OK."
    exit 0
}

