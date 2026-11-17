$ErrorActionPreference = 'Stop'
Import-Module "$PSHOME/Modules/Microsoft.PowerShell.Utility/Microsoft.PowerShell.Utility.psd1"
Import-Module "$PSHOME/Modules/Microsoft.PowerShell.Archive/Microsoft.PowerShell.Archive.psd1"

$temurinTag = 'jdk-17.0.21+9'
$archiveName = 'OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip'
$expectedHash = 'fe4656bd78fb09812dc38c49da777f79cc33e7ee435a254be82f9e7f40d9d117'
$downloadUrl = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/' + $archiveName
$cache = Join-Path (Split-Path -Parent $PSScriptRoot) 'src-tauri/target/java-runtime'

New-Item -ItemType Directory -Path $cache -Force | Out-Null
$archivePath = Join-Path $cache $archiveName
if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf) -or
    (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash -ne $expectedHash) {
    & curl.exe --fail --location --retry 3 --silent --show-error --output $archivePath $downloadUrl
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not download Eclipse Temurin Java 17.'
    }
}

if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Eclipse Temurin Java 17 archive failed SHA-256 validation.'
}
Expand-Archive -LiteralPath $archivePath -DestinationPath $cache -Force

$runtime = Join-Path $cache "$temurinTag-jre"
if (-not (Test-Path -LiteralPath (Join-Path $runtime 'bin/java.exe') -PathType Leaf)) {
    throw 'Eclipse Temurin archive did not contain the bundled Java launcher.'
}
