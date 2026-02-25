$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

function Get-LocalIPv4 {
    $candidates = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike "169.254.*" -and
            $_.IPAddress -ne "127.0.0.1" -and
            $_.PrefixOrigin -ne "WellKnown"
        } |
        Select-Object -ExpandProperty IPAddress

    if ($candidates -and $candidates.Count -gt 0) {
        return $candidates[0]
    }

    return "localhost"
}

$ip = Get-LocalIPv4

Write-Host ""
Write-Host "Iniciando backend (perfil local) y frontend en modo LAN..."
Write-Host "URL para el celular: http://$ip`:5173"
Write-Host ""

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\frontend'; npm run dev:lan"
