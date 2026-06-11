param(
    [string]$ProductBaseUrl = "http://localhost:8082",
    [string]$OrderBaseUrl = "http://localhost:8083",
    [string]$Sku = "TSN-001-M-WHT",
    [int]$Quantity = 1,
    [string]$UserId = "11111111-1111-1111-1111-111111111111",
    [ValidateSet("COD", "VNPAY")]
    [string]$PaymentMethod = "COD",
    [int]$TimeoutSeconds = 30
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
    }

    $json = $Body | ConvertTo-Json -Depth 12
    return Invoke-RestMethod -Method $Method -Uri $Uri -ContentType "application/json" -Body $json -Headers $Headers
}

Write-Host "Checking product variant $Sku ..."
$escapedSku = [System.Uri]::EscapeDataString($Sku)
$variantResponse = Invoke-Json -Method "GET" -Uri "$ProductBaseUrl/api/internal/products/variants/$escapedSku"
if (-not $variantResponse.success) {
    throw "Product variant lookup failed: $($variantResponse.message)"
}

$variant = $variantResponse.data
Write-Host "Variant OK: $($variant.productName), price=$($variant.price), stock=$($variant.stock)"

$shippingAddress = @{
    fullName = "Demo Customer"
    phone = "0900000000"
    street = "1 Demo Street"
    district = "District 1"
    city = "Ho Chi Minh City"
} | ConvertTo-Json -Compress

$checkoutPayload = @{
    userId = $UserId
    lines = @(
        @{
            sku = $Sku
            quantity = $Quantity
        }
    )
    shippingAddress = $shippingAddress
    paymentMethod = $PaymentMethod
}

Write-Host "Creating order ..."
$checkoutResponse = Invoke-Json -Method "POST" -Uri "$OrderBaseUrl/api/orders/checkout/by-sku" -Body $checkoutPayload
if (-not $checkoutResponse.success) {
    throw "Checkout failed: $($checkoutResponse.message)"
}

$orderId = $checkoutResponse.data.orderId
Write-Host "Order created: $orderId"

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$statusResponse = $null

do {
    Start-Sleep -Seconds 1
    $statusResponse = Invoke-Json -Method "GET" -Uri "$OrderBaseUrl/api/orders/$orderId/status" -Headers @{ "X-User-Id" = $UserId }
    $data = $statusResponse.data
    $sagaStatus = if ($data.saga) { $data.saga.status } else { "NONE" }
    $currentStep = if ($data.saga) { $data.saga.currentStep } else { "NONE" }

    Write-Host "Order=$($data.status), payment=$($data.paymentStatus), saga=$sagaStatus/$currentStep"

    if ($data.status -eq "CONFIRMED" -or $data.status -eq "CANCELLED" -or
        $sagaStatus -eq "COMPLETED" -or $sagaStatus -eq "FAILED") {
        break
    }
} while ((Get-Date) -lt $deadline)

Write-Host "Final order detail:"
$detailResponse = Invoke-Json -Method "GET" -Uri "$OrderBaseUrl/api/orders/$orderId" -Headers @{ "X-User-Id" = $UserId }
$detailResponse.data | ConvertTo-Json -Depth 12

if ($statusResponse.data.status -eq "PENDING") {
    Write-Host "Order is still PENDING. Check that product-service, order-service, payment-service, and RabbitMQ are all running."
}
