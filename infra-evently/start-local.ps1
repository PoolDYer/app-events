param([switch]$ConfigureOnly)

$ErrorActionPreference = 'Stop'

function Invoke-Docker {
    docker @args
    if ($LASTEXITCODE -ne 0) {
        throw "Docker terminó con código $LASTEXITCODE al ejecutar $($args[0])."
    }
}

Push-Location $PSScriptRoot
try {
    if (-not (Test-Path -LiteralPath '.env')) {
        $settings = Get-Content -LiteralPath '.env.example'
        $settings = foreach ($line in $settings) {
            if ($line -match '^(MYSQL_ROOT_PASSWORD|HTTP_JWT_ACCESS_SECRET|HTTP_JWT_REFRESH_SECRET)=$') {
                $secret = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
                $line + $secret
            } else {
                $line
            }
        }
        Set-Content -LiteralPath '.env' -Value $settings -Encoding utf8
    }
    if ($ConfigureOnly) { return }
    Invoke-Docker info --format '{{.ServerVersion}}'
    $settings = Get-Content -LiteralPath '.env' -Raw | ConvertFrom-StringData
    $network = $settings.NETWORK.Trim('"', "'")
    if ($network -notin @(Invoke-Docker network ls --format '{{.Name}}')) {
        Invoke-Docker network create $network
    }
    Invoke-Docker compose -p $settings.PROJECT_NAME -f docker-compose.mysql.yml -f docker-compose.redis.yml -f docker-compose.http.yml -f docker-compose.web.yml up -d --build --wait
} finally {
    Pop-Location
}
