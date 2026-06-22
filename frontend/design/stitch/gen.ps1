# Stitch screen generator — calls the Stitch MCP HTTP endpoint directly (JSON-RPC
# tools/call) so we don't echo the whole design system back through the agent
# context. Downloads the generated HTML + screenshot to this folder and prints a
# short summary line. Usage:
#   $env:STITCH_API_KEY="..."; ./gen.ps1 -Name "02-product-list" -Device DESKTOP -PromptFile prompt.txt
param(
  [Parameter(Mandatory = $true)][string]$Name,
  [string]$Device = "DESKTOP",
  [string]$PromptFile,
  [string]$Prompt
)

$ErrorActionPreference = "Stop"
$projectId = "5924587690045058307"
$designSystem = "assets/17792112929796079019"
$url = "https://stitch.googleapis.com/mcp"
$key = $env:STITCH_API_KEY
$dir = $PSScriptRoot

if ($PromptFile) { $Prompt = Get-Content -Raw -Path $PromptFile }
if (-not $Prompt) { throw "Provide -Prompt or -PromptFile" }

$args = @{
  projectId    = $projectId
  designSystem = $designSystem
  deviceType   = $Device
  modelId      = "GEMINI_3_1_PRO"
  prompt       = $Prompt
}
$body = @{
  jsonrpc = "2.0"; id = 1; method = "tools/call"
  params  = @{ name = "generate_screen_from_text"; arguments = $args }
} | ConvertTo-Json -Depth 20

$headers = @{
  "X-Goog-Api-Key" = $key
  "Accept"         = "application/json, text/event-stream"
  "Content-Type"   = "application/json"
}

$resp = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -TimeoutSec 360
# tools/call result: result.content[].text holds the JSON payload string
$textPart = $resp.result.content | Where-Object { $_.type -eq "text" } | Select-Object -First 1
if (-not $textPart) { throw "No text content in response: $($resp | ConvertTo-Json -Depth 8)" }
$payload = $textPart.text | ConvertFrom-Json

# Find the screen object
$screen = $null
foreach ($oc in $payload.outputComponents) {
  if ($oc.design -and $oc.design.screens) { $screen = $oc.design.screens[0]; break }
}
if (-not $screen) { throw "No screen in payload: $($textPart.text.Substring(0,[Math]::Min(400,$textPart.text.Length)))" }

$htmlUrl = $screen.htmlCode.downloadUrl
$shotUrl = $screen.screenshot.downloadUrl
Invoke-WebRequest -Uri $htmlUrl -OutFile "$dir\$Name.html" -UseBasicParsing
if ($shotUrl) { try { Invoke-WebRequest -Uri $shotUrl -OutFile "$dir\$Name.png" -UseBasicParsing } catch {} }

$htmlLen = (Get-Item "$dir\$Name.html").Length
# Append to manifest
$manifestLine = "$Name`t$($screen.id)`t$($screen.title)`t$htmlLen"
Add-Content -Path "$dir\manifest.tsv" -Value $manifestLine
Write-Output "OK $Name | id=$($screen.id) | title=$($screen.title) | html=$htmlLen bytes"
