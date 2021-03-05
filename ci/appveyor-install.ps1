$ErrorActionPreference = 'Stop';

# comment for invalidating install cache

# download graalvm package if not exist
$url = "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.0.0.2/graalvm-ce-java11-windows-amd64-21.0.0.2.zip"
$filePath = "graalvm-21.0.zip"

if (Test-Path($env:GRAALVM_HOME))
{
    Write-Host 'Skipping graalvm download, already downloaded' -ForegroundColor Yellow
    return
}

Invoke-WebRequest $url -OutFile $filePath

Expand-Archive -LiteralPath $filePath -DestinationPath "."
Move-Item "graalvm-ce-java11-21.0.0.2" $env:GRAALVM_HOME
