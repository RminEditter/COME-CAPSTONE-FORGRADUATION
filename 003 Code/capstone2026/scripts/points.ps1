param([switch]$Apply)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCommand) { $pointNode = $nodeCommand.Source } else {
    $pointNode = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
}
if (!(Test-Path -LiteralPath $pointNode)) { throw 'Node.js is required.' }
$pointScript = Join-Path $PSScriptRoot 'award-points.cjs'
if ($Apply) { & $pointNode $pointScript --apply } else { & $pointNode $pointScript }
if ($LASTEXITCODE -ne 0) { throw 'Point processing failed. Review the error above; rerunning is safe.' }
