# Generate ALL Cartzilla screens from docs/FRONTEND-STITCH-PROMPTS.md via Stitch.
# Parses each "## N. Title" section's fenced prompt block, calls Stitch
# generate_screen_from_text, and downloads the HTML + screenshot into this folder.
# Resumable: skips a screen if its .html already exists. Logs to gen.log.
$ErrorActionPreference = "Stop"
$projectId = "5924587690045058307"
$designSystem = "assets/17792112929796079019"
$url = "https://stitch.googleapis.com/mcp"
$key = $env:STITCH_API_KEY
$dir = $PSScriptRoot
$promptsDoc = "D:\Workspace\FPT\Summer2026\MSS301\cartzilla\docs\FRONTEND-STITCH-PROMPTS.md"
$log = "$dir\gen.log"

function Log($msg) {
  $line = "[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $msg
  Add-Content -Path $log -Value $line
  Write-Output $line
}

# Dashboard (staff/admin) screens — generate the rest as desktop storefront.
$dashboardNums = @(19, 20, 21, 22, 23, 24, 25, 26, 27)

function Slug([string]$s) {
  $s = $s.ToLower() -replace '[^a-z0-9]+', '-'
  return $s.Trim('-')
}

# --- Parse the markdown into screen { num, title, prompt } records ---
# NOTE: read as UTF-8 explicitly — Windows PowerShell 5.1 Get-Content otherwise
# decodes the file with the system ANSI codepage and mangles Vietnamese text.
$lines = Get-Content -Path $promptsDoc -Encoding UTF8
$screens = @()
$curHeading = $null
$inFence = $false
$buf = New-Object System.Collections.Generic.List[string]
foreach ($ln in $lines) {
  if ($ln -match '^##\s+(\d+)\.\s+(.+?)\s*$') {
    $curHeading = [pscustomobject]@{ num = [int]$Matches[1]; title = $Matches[2] }
    continue
  }
  if ($ln -match '^##\s') { $curHeading = $null; continue }   # non-numbered heading (e.g. Prompt 0)
  if ($ln -match '^\s*```') {
    if (-not $inFence) { $inFence = $true; $buf.Clear() }
    else {
      $inFence = $false
      if ($curHeading) {
        $screens += [pscustomobject]@{ num = $curHeading.num; title = $curHeading.title; prompt = ($buf -join "`n") }
        $curHeading = $null
      }
    }
    continue
  }
  if ($inFence) { $buf.Add($ln) }
}

Log "Parsed $($screens.Count) screen prompts."

$headers = @{
  "X-Goog-Api-Key" = $key
  "Accept"         = "application/json, text/event-stream"
  "Content-Type"   = "application/json"
}

foreach ($sc in ($screens | Sort-Object num)) {
  $name = "{0:D2}-{1}" -f $sc.num, (Slug $sc.title)
  $htmlPath = "$dir\$name.html"
  if (Test-Path $htmlPath) { Log "SKIP $name (exists)"; continue }

  $device = if ($dashboardNums -contains $sc.num) { "DESKTOP" } else { "DESKTOP" }
  $arguments = @{
    projectId = $projectId; designSystem = $designSystem; deviceType = $device
    modelId = "GEMINI_3_1_PRO"; prompt = $sc.prompt
  }
  $bodyObj = @{ jsonrpc = "2.0"; id = $sc.num; method = "tools/call"
    params = @{ name = "generate_screen_from_text"; arguments = $arguments } }
  # Send as explicit UTF-8 bytes — passing a string body lets PS 5.1 transmit it
  # as Latin-1, corrupting the ₫/Vietnamese chars so the server rejects the JSON.
  $body = [System.Text.Encoding]::UTF8.GetBytes(($bodyObj | ConvertTo-Json -Depth 20))

  $attempt = 0; $ok = $false
  while (-not $ok -and $attempt -lt 2) {
    $attempt++
    try {
      Log "GEN $name (attempt $attempt)..."
      $resp = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -TimeoutSec 420
      $textPart = $resp.result.content | Where-Object { $_.type -eq "text" } | Select-Object -First 1
      if (-not $textPart) { throw "no text content" }
      $payload = $textPart.text | ConvertFrom-Json
      $screen = $null
      foreach ($oc in $payload.outputComponents) { if ($oc.design -and $oc.design.screens) { $screen = $oc.design.screens[0]; break } }
      if (-not $screen) { throw "no screen object" }
      Invoke-WebRequest -Uri $screen.htmlCode.downloadUrl -OutFile $htmlPath -UseBasicParsing
      if ($screen.screenshot.downloadUrl) { try { Invoke-WebRequest -Uri $screen.screenshot.downloadUrl -OutFile "$dir\$name.png" -UseBasicParsing } catch {} }
      $len = (Get-Item $htmlPath).Length
      Add-Content -Path "$dir\manifest.tsv" -Value ("{0}`t{1}`t{2}`t{3}" -f $name, $screen.id, $screen.title, $len)
      Log "OK $name | id=$($screen.id) | $($screen.title) | $len bytes"
      $ok = $true
    } catch {
      Log "ERR $name attempt $attempt : $($_.Exception.Message)"
      Start-Sleep -Seconds 5
    }
  }
  if (-not $ok) { Log "FAIL $name after $attempt attempts" }
}
Log "DONE all screens."
