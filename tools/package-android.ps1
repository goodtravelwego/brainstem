# Export source from an explicit allowlist; never copy credentials, local SDK paths, or caches.
[CmdletBinding()]
param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location -LiteralPath $projectRoot
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat :app:assembleDebug
        if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    }
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'BrainSTEM-Stimulate-debug.apk'
    $archivePath = Join-Path $projectRoot 'BrainSTEM-Stimulate-Android.zip'
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $files = @('.editorconfig', '.gitattributes', '.gitignore', 'README.md', 'CONTRIBUTING.md',
        'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'app/build.gradle.kts', 'reference-app.html')
    foreach ($directory in @('app/src', 'gradle', 'docs', 'reference', 'tools', '.github')) {
        $trackedFiles = & git ls-files -- $directory
        if ($LASTEXITCODE -ne 0) { throw 'Git is required to export reviewed source files.' }
        $files += $trackedFiles
    }
    $stream = [IO.File]::Open($archivePath, [IO.FileMode]::Create)
    try {
        $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($relative in $files) {
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,
                    (Join-Path $projectRoot $relative), $relative.Replace('\', '/'),
                    [IO.Compression.CompressionLevel]::Optimal) | Out-Null
            }
        } finally { $archive.Dispose() }
    } finally { $stream.Dispose() }
    Get-FileHash -Algorithm SHA256 -LiteralPath 'BrainSTEM-Stimulate-debug.apk', $archivePath |
        Select-Object Path, Hash
} finally { Pop-Location }
