param(
  [int]$Port = 8087
)

$ErrorActionPreference = "Stop"

function Assert-EndpointOk {
  param([string]$Url)

  Write-Host "Test: $Url"
  try {
    # On utilise Invoke-WebRequest pour récupérer le code HTTP
    $r = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 10 -UseBasicParsing
    Write-Host "OK ($($r.StatusCode))"
  } catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $code = [int]$_.Exception.Response.StatusCode
      throw "KO ($code) sur $Url"
    }
    throw "KO sur $Url : $($_.Exception.Message)"
  }
}

# IMPORTANT: tes endpoints sont sous /api (context-path)
# Le code actuel expose /api/activities et /api/journal.
# On teste d'abord les endpoints "souhaités", puis on fallback sur ceux existants si 404.
$candidates = @(
  "http://localhost:$Port/api/cognitive-activities",
  "http://localhost:$Port/api/activities"
)

$journalCandidates = @(
  "http://localhost:$Port/api/journals",
  "http://localhost:$Port/api/journal"
)

function Test-WithFallback {
  param([string[]]$Urls)
  foreach ($u in $Urls) {
    try {
      Assert-EndpointOk -Url $u
      return
    } catch {
      # continue vers l'url suivante
    }
  }
  throw "Aucun endpoint valide trouvé parmi: $($Urls -join ', ')"
}

Test-WithFallback -Urls $candidates
Test-WithFallback -Urls $journalCandidates

Write-Host "Tous les tests endpoints ont réussi."

