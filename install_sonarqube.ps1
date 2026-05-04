$home = $env:USERPROFILE
$url = "https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.6.0.92116.zip"
$zipPath = "$home\sonarqube-10.6.0.zip"
$sonarDir = "$home\sonarqube"

Write-Host "Downloading SonarQube Community Edition 10.6.0 to $zipPath ..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
Write-Host "Download complete!" -ForegroundColor Green

Write-Host "Extracting to $home ..." -ForegroundColor Cyan
Expand-Archive -Path $zipPath -DestinationPath $home -Force

# Rename extracted folder to sonarqube
$extractedFolder = Get-ChildItem $home -Directory | Where-Object { $_.Name -like "sonarqube-*" } | Select-Object -First 1
if ($extractedFolder) {
    if (Test-Path $sonarDir) { Remove-Item $sonarDir -Recurse -Force }
    Rename-Item -Path $extractedFolder.FullName -NewName "sonarqube" -Force
}

Write-Host "SonarQube extracted to $sonarDir" -ForegroundColor Green
Write-Host "Starting SonarQube..." -ForegroundColor Cyan
Start-Process "$sonarDir\bin\windows-x86-64\StartSonar.bat"
Write-Host "SonarQube starting on http://localhost:9000 (wait ~1-2 min)" -ForegroundColor Green
