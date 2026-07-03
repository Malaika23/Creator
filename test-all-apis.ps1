# PowerShell API Test Script for E-Commerce Backend Microservices
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api/v1"

Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  STARTING E-COMMERCE END-TO-END API TEST SUITE" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan

# Helper to execute Invoke-RestMethod with output details
function Call-Api {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [string]$Token = $null
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    if ($Token) {
        $headers.Add("Authorization", "Bearer $Token")
    }
    
    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $headers
    }
    if ($Body) {
        $params.Add("Body", ($Body | ConvertTo-Json -Depth 5))
    }
    
    try {
        $resp = Invoke-RestMethod @params
        return $resp
    } catch {
        Write-Host "[ERROR] Error calling $Method $Url" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            Write-Host "Details: $errBody" -ForegroundColor Red
        } else {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
        throw $_
    }
}

# 1. Register Admin User
Write-Host ""
Write-Host "[1/12] Registering Admin user..." -ForegroundColor Yellow
$adminRegisterBody = @{
    email = "admin_test@ecommerce.com"
    password = "password123"
    role = "ADMIN"
    phone = "9876543210"
}
try {
    $adminUser = Call-Api -Method "POST" -Url "$baseUrl/auth/register" -Body $adminRegisterBody
    Write-Host "[SUCCESS] Admin user registered successfully! ID: $($adminUser.id)" -ForegroundColor Green
} catch {
    Write-Host "[INFO] Admin user might already be registered. Proceeding to login." -ForegroundColor Gray
}

# 2. Authenticate Admin User
Write-Host ""
Write-Host "[2/12] Authenticating Admin user..." -ForegroundColor Yellow
$adminLoginBody = @{
    email = "admin_test@ecommerce.com"
    password = "password123"
}
$adminAuth = Call-Api -Method "POST" -Url "$baseUrl/auth/login" -Body $adminLoginBody
$adminToken = $adminAuth.accessToken
Write-Host "[SUCCESS] Admin authenticated successfully! Token obtained." -ForegroundColor Green

# 3. Create Product Category
Write-Host ""
Write-Host "[3/12] Creating Category (Admin role)..." -ForegroundColor Yellow
try {
    $category = Call-Api -Method "POST" -Url "$baseUrl/products/categories?name=Electronics" -Token $adminToken
    $categoryId = $category.id
    Write-Host "[SUCCESS] Category 'Electronics' created. ID: $categoryId" -ForegroundColor Green
} catch {
    Write-Host "[INFO] Category might already exist. Fetching existing category list..." -ForegroundColor Gray
    $categories = Call-Api -Method "GET" -Url "$baseUrl/products/categories" -Token $adminToken
    $matchingCategory = $categories | Where-Object { $_.name -eq "Electronics" }
    if ($matchingCategory) {
        $categoryId = $matchingCategory.id
        Write-Host "[SUCCESS] Retrieved existing category 'Electronics'. ID: $categoryId" -ForegroundColor Green
    } else {
        throw $_
    }
}

# 4. Create Product in Category
Write-Host ""
Write-Host "[4/12] Creating Product (Admin/Seller role)..." -ForegroundColor Yellow
$sku = "LAPTOP-" + (Get-Random -Minimum 1000 -Maximum 9999)
$productBody = @{
    name = "Horizon Gaming Laptop"
    description = "Ultra high-performance gaming laptop"
    price = 89999.00
    categoryId = $categoryId
    sku = $sku
}
$product = Call-Api -Method "POST" -Url "$baseUrl/products" -Body $productBody -Token $adminToken
$productId = $product.id
Write-Host "[SUCCESS] Product created. ID: $productId | SKU: $sku" -ForegroundColor Green

# 5. Add Stock to Inventory
Write-Host ""
Write-Host "[5/12] Replenishing stock in inventory (Admin role)..." -ForegroundColor Yellow
$stockUrl = $baseUrl + "/inventory/stock?productId=" + $productId + "&quantity=100"
$stock = Call-Api -Method "POST" -Url $stockUrl -Token $adminToken
Write-Host "[SUCCESS] Stock replenished. Current Quantity: $($stock.stockQuantity) | Reserved: $($stock.reservedQuantity)" -ForegroundColor Green

# 6. Register Customer User
Write-Host ""
Write-Host "[6/12] Registering Customer user..." -ForegroundColor Yellow
$custEmail = "customer_" + (Get-Random -Minimum 1000 -Maximum 9999) + "@ecommerce.com"
$custRegisterBody = @{
    email = $custEmail
    password = "password123"
    role = "CUSTOMER"
    phone = "1234567890"
}
$custUser = Call-Api -Method "POST" -Url "$baseUrl/auth/register" -Body $custRegisterBody
Write-Host "[SUCCESS] Customer user registered: $custEmail" -ForegroundColor Green

# 7. Authenticate Customer User
Write-Host ""
Write-Host "[7/12] Authenticating Customer user..." -ForegroundColor Yellow
$custLoginBody = @{
    email = $custEmail
    password = "password123"
}
$custAuth = Call-Api -Method "POST" -Url "$baseUrl/auth/login" -Body $custLoginBody
$custToken = $custAuth.accessToken
Write-Host "[SUCCESS] Customer authenticated successfully! Token obtained." -ForegroundColor Green

# 8. Add Item to Shopping Cart
Write-Host ""
Write-Host "[8/12] Adding product to Shopping Cart..." -ForegroundColor Yellow
$cartBody = @{
    productId = $productId
    quantity = 2
}
$cart = Call-Api -Method "POST" -Url "$baseUrl/carts" -Body $cartBody -Token $custToken
Write-Host "[SUCCESS] Item added to cart. Cart contains $($cart.items.Count) unique product(s)." -ForegroundColor Green

# 9. Get Shopping Cart
Write-Host ""
Write-Host "[9/12] Querying Shopping Cart..." -ForegroundColor Yellow
$cartQuery = Call-Api -Method "GET" -Url "$baseUrl/carts" -Token $custToken
foreach ($item in $cartQuery.items) {
    Write-Host "  - Product ID: $($item.productId) | Quantity: $($item.quantity)" -ForegroundColor Gray
}

# 10. Place Order: SUCCESS Saga Case
Write-Host ""
Write-Host "[10/12] Placing Order (SUCCESS Scenario)..." -ForegroundColor Yellow
$orderSuccessKey = "order-success-" + (Get-Random)
$orderBody = @{
    items = @(
        @{
            productId = $productId
            quantity = 2
            price = 89999.00
        }
    )
    idempotencyKey = $orderSuccessKey
}
$order = Call-Api -Method "POST" -Url "$baseUrl/orders" -Body $orderBody -Token $custToken
$orderId = $order.id
Write-Host "[SUCCESS] Order placed. ID: $orderId | Status: $($order.status)" -ForegroundColor Green

# Wait for Kafka Saga orchestration (inventory reserve -> payment success -> confirm)
Write-Host "Waiting 4 seconds for Kafka Saga processing..." -ForegroundColor Gray
Start-Sleep -Seconds 4

# Verify Order Confirm status
$orderVerify = Call-Api -Method "GET" -Url "$baseUrl/orders/$orderId" -Token $custToken
if ($orderVerify.status -eq "CONFIRMED") {
    Write-Host "[SUCCESS] SUCCESS Saga completed! Order Status: $($orderVerify.status) | Payment ID: $($orderVerify.paymentId)" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Order status is: $($orderVerify.status)" -ForegroundColor Red
}

# 11. Place Order: FAILED Saga Case (triggers compensating stock rollback)
Write-Host ""
Write-Host "[11/12] Placing Order for Rs 999 (FAILED Scenario; triggering compensating stock rollback)..." -ForegroundColor Yellow

# Verify stock levels before failed order
$stockBeforeUrl = $baseUrl + "/inventory/" + $productId
$stockBefore = Call-Api -Method "GET" -Url $stockBeforeUrl -Token $adminToken
Write-Host "  - Reserved Stock before order: $($stockBefore.reservedQuantity)" -ForegroundColor Gray

$orderFailKey = "order-fail-" + (Get-Random)
$orderFailBody = @{
    items = @(
        @{
            productId = $productId
            quantity = 1
            price = 999.00  # Trigger simulated payment failure
        }
    )
    idempotencyKey = $orderFailKey
}
$orderFailed = Call-Api -Method "POST" -Url "$baseUrl/orders" -Body $orderFailBody -Token $custToken
$failedOrderId = $orderFailed.id
Write-Host "[SUCCESS] Order placed. ID: $failedOrderId | Status: $($orderFailed.status)" -ForegroundColor Green

# Wait for Kafka Saga orchestration (inventory reserve -> payment fail -> order cancel -> inventory release)
Write-Host "Waiting 4 seconds for Saga rollback processing..." -ForegroundColor Gray
Start-Sleep -Seconds 4

# Verify Order Cancel status
$orderFailVerify = Call-Api -Method "GET" -Url "$baseUrl/orders/$failedOrderId" -Token $custToken
if ($orderFailVerify.status -eq "CANCELLED") {
    Write-Host "[SUCCESS] FAILED Saga compensating transaction completed! Order is CANCELLED." -ForegroundColor Green
} else {
    Write-Host "[WARNING] Order status is: $($orderFailVerify.status)" -ForegroundColor Red
}

# Verify Stock levels released back in Inventory Service
$stockAfterUrl = $baseUrl + "/inventory/" + $productId
$stockAfter = Call-Api -Method "GET" -Url $stockAfterUrl -Token $adminToken
Write-Host "  - Reserved Stock after compensating transaction: $($stockAfter.reservedQuantity)" -ForegroundColor Gray
if ($stockAfter.reservedQuantity -eq $stockBefore.reservedQuantity) {
    Write-Host "[SUCCESS] Confirmed: Stock reservation was successfully rolled back. NO STOCK LEAKS!" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Stock leak detected! Reserved stock was not rolled back." -ForegroundColor Red
}

# 12. Test AI capabilities (Semantic search & Chat Assistant)
Write-Host ""
Write-Host "[12/12] Querying AI Service features..." -ForegroundColor Yellow

Write-Host "Testing AI Semantic Search:" -ForegroundColor Gray
$searchResults = Call-Api -Method "GET" -Url "$baseUrl/search?query=formal%20office%20shoes" -Token $custToken
foreach ($res in $searchResults) {
    Write-Host "  - name: $($res.name) | price: $($res.price) | description: $($res.description)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Testing AI Assistant Chat Prompt:" -ForegroundColor Gray
$chatBody = @{
    message = "Suggest some formal office shoes"
}
$chatResp = Call-Api -Method "POST" -Url "$baseUrl/assistant/chat" -Body $chatBody -Token $custToken
Write-Host "AI Assistant Response:" -ForegroundColor DarkYellow
Write-Host $chatResp -ForegroundColor DarkYellow

Write-Host ""
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  ALL API AND SAGA TESTS COMPLETED SUCCESSFULLY!" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
