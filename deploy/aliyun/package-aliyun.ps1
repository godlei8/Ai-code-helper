Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir '..\..')
$releaseRoot = Join-Path $repoRoot 'release'
$bundleName = 'ai-code-helper-aliyun'
$bundleRoot = Join-Path $releaseRoot $bundleName
$backendSource = Join-Path $repoRoot 'target'
$frontendRoot = Join-Path $repoRoot 'ai-code-helper-frontend'
$frontendDist = Join-Path $frontendRoot 'dist'
$deploySource = Join-Path $repoRoot 'deploy\aliyun'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$zipPath = Join-Path $releaseRoot "$bundleName-$timestamp.zip"

if (Test-Path $bundleRoot) {
    Remove-Item $bundleRoot -Recurse -Force
}

if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

if (-not (Test-Path $releaseRoot)) {
    New-Item -ItemType Directory -Path $releaseRoot | Out-Null
}

New-Item -ItemType Directory -Path $bundleRoot | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'backend\config') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'frontend') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'nginx') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'scripts') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $bundleRoot 'systemd') -Force | Out-Null

Push-Location $repoRoot
try {
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Backend package failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$jar = Get-ChildItem $backendSource -Filter '*.jar' | Where-Object { $_.Name -notlike '*.original' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $jar) {
    throw 'Backend jar was not generated.'
}

Push-Location $frontendRoot
try {
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $frontendDist)) {
    throw 'Frontend dist directory was not generated.'
}

Copy-Item $jar.FullName (Join-Path $bundleRoot 'backend\ai-code-helper.jar') -Force
Copy-Item (Join-Path $deploySource 'backend\application-prod.yml.example') (Join-Path $bundleRoot 'backend\config\application-prod.yml.example') -Force
Copy-Item (Join-Path $deploySource 'README.md') (Join-Path $bundleRoot 'README.md') -Force
Copy-Item (Join-Path $deploySource 'nginx\ai-code-helper.conf') (Join-Path $bundleRoot 'nginx\ai-code-helper.conf') -Force
Copy-Item (Join-Path $deploySource 'scripts\start-backend.sh') (Join-Path $bundleRoot 'scripts\start-backend.sh') -Force
Copy-Item (Join-Path $deploySource 'scripts\stop-backend.sh') (Join-Path $bundleRoot 'scripts\stop-backend.sh') -Force
Copy-Item (Join-Path $deploySource 'systemd\ai-code-helper.service') (Join-Path $bundleRoot 'systemd\ai-code-helper.service') -Force
Copy-Item (Join-Path $frontendDist '*') (Join-Path $bundleRoot 'frontend') -Recurse -Force

Compress-Archive -Path $bundleRoot -DestinationPath $zipPath -Force

Write-Host "Aliyun deployment package created: $zipPath"
