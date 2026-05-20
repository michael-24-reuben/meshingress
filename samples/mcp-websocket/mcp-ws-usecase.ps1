param(
    [ValidateSet("Create", "Rotate", "Send", "Delete", "Show")]
    [string] $Action = "Show",

    [string] $Method = "ping",

    [string] $JsonRpc,

    [string] $Uri = "ws://localhost:8080/mcp/ws",

    [string] $OutputDirectory = "temp/mcp-ws-demo",

    [switch] $Admin,

    [switch] $UseInvalidCredentials
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
    $current = Get-Location
    while ($current -and -not (Test-Path (Join-Path $current "mvnw.cmd"))) {
        $current = Split-Path -Parent $current
    }
    if (-not $current) {
        throw "Could not locate repo root. Run this script from inside the meshingress checkout."
    }
    return (Resolve-Path $current).Path
}

function New-DemoToken {
    param([string] $Prefix)
    return "$Prefix-$([Guid]::NewGuid().ToString('N'))"
}

function Get-DemoConfigPath {
    param([string] $RepoRoot, [string] $Directory)
    return Join-Path (Join-Path $RepoRoot $Directory) "application-mcp-ws-demo.properties"
}

function New-DemoCredentials {
    param([string] $Path)

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory | Out-Null
    }

    $accessToken = New-DemoToken "access"
    $secretKey = New-DemoToken "secret"
    $authToken = New-DemoToken "auth"
    $clientId = "demo-client"
    $subject = "demo-subject"

    @"
meshingress.mcp.auth.stub.enabled=true
meshingress.mcp.auth.stub.access-token=$accessToken
meshingress.mcp.auth.stub.secret-key=$secretKey
meshingress.mcp.auth.stub.auth-token=$authToken
meshingress.mcp.auth.stub.client-id=$clientId
meshingress.mcp.auth.stub.subject=$subject
"@ | Set-Content -Path $Path -Encoding UTF8

    return Read-DemoCredentials $Path
}

function Read-DemoCredentials {
    param([string] $Path)

    if (-not (Test-Path $Path)) {
        throw "Demo credential file does not exist: $Path. Run with -Action Create first."
    }

    $values = @{}
    Get-Content $Path | ForEach-Object {
        if ($_ -match "^\s*#" -or $_ -notmatch "=") {
            return
        }
        $key, $value = $_ -split "=", 2
        $values[$key.Trim()] = $value.Trim()
    }

    return [pscustomobject]@{
        AccessToken = $values["meshingress.mcp.auth.stub.access-token"]
        SecretKey = $values["meshingress.mcp.auth.stub.secret-key"]
        AuthToken = $values["meshingress.mcp.auth.stub.auth-token"]
        ClientId = $values["meshingress.mcp.auth.stub.client-id"]
        Subject = $values["meshingress.mcp.auth.stub.subject"]
    }
}

function Show-RunCommand {
    param([string] $RepoRoot, [string] $Path)

    $configUri = ([Uri] (Resolve-Path $Path).Path).AbsoluteUri
    Write-Host ""
    Write-Host "Start the server with these demo credentials:"
    Write-Host ""
    Write-Host ".\mvnw.cmd -pl app/meshingress-server -am spring-boot:run `"-Dspring-boot.run.arguments=--spring.config.additional-location=$configUri`""
    Write-Host ""
}

function New-JsonRpcPayload {
    param([string] $Method, [string] $JsonRpc)

    if ($JsonRpc) {
        return $JsonRpc
    }

    switch ($Method) {
        "initialize" {
            return '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"meshingress-ws-sample","version":"0.1.0"}}}'
        }
        "roles/tools/list" {
            return '{"jsonrpc":"2.0","id":2,"method":"roles/tools/list","params":{"includeDisabled":true,"includePrivate":true}}'
        }
        default {
            return (@{
                jsonrpc = "2.0"
                id = 1
                method = $Method
                params = @{}
            } | ConvertTo-Json -Compress -Depth 10)
        }
    }
}

function Receive-WebSocketText {
    param([System.Net.WebSockets.ClientWebSocket] $Socket)

    $buffer = [byte[]]::new(8192)
    $segments = New-Object System.Collections.Generic.List[byte]

    do {
        $segment = [ArraySegment[byte]]::new($buffer)
        $result = $Socket.ReceiveAsync($segment, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
        if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
            return ""
        }
        for ($i = 0; $i -lt $result.Count; $i++) {
            $segments.Add($buffer[$i])
        }
    } while (-not $result.EndOfMessage)

    return [Text.Encoding]::UTF8.GetString($segments.ToArray())
}

function Send-McpRequest {
    param(
        [string] $Uri,
        [object] $Credentials,
        [string] $Payload,
        [bool] $Admin,
        [bool] $UseInvalidCredentials
    )

    $socket = [System.Net.WebSockets.ClientWebSocket]::new()
    try {
        $accessToken = $Credentials.AccessToken
        $secretKey = $Credentials.SecretKey
        $authToken = $Credentials.AuthToken

        if ($UseInvalidCredentials) {
            $accessToken = "invalid-access-token"
            $secretKey = "invalid-secret-key"
            $authToken = "invalid-auth-token"
        }

        $socket.Options.SetRequestHeader("Authorization", "Bearer $accessToken")
        $socket.Options.SetRequestHeader("X-Secret-Key", $secretKey)
        $socket.Options.SetRequestHeader("X-Auth-Token", $authToken)
        $socket.Options.SetRequestHeader("Mcp-Session-Id", "sample-session-1")
        $socket.Options.SetRequestHeader("X-Request-Id", "sample-request-1")
        if ($Admin) {
            $socket.Options.SetRequestHeader("X-Mcp-Role", "admin")
        }

        $socket.ConnectAsync([Uri] $Uri, [Threading.CancellationToken]::None).GetAwaiter().GetResult()
        $bytes = [Text.Encoding]::UTF8.GetBytes($Payload)
        $socket.SendAsync(
            [ArraySegment[byte]]::new($bytes),
            [System.Net.WebSockets.WebSocketMessageType]::Text,
            $true,
            [Threading.CancellationToken]::None
        ).GetAwaiter().GetResult()

        $response = Receive-WebSocketText $socket
        if ($response) {
            Write-Host $response
        } else {
            Write-Host "No response frame returned."
        }
    } finally {
        if ($socket.State -eq [System.Net.WebSockets.WebSocketState]::Open) {
            $socket.CloseAsync(
                [System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure,
                "sample complete",
                [Threading.CancellationToken]::None
            ).GetAwaiter().GetResult()
        }
        $socket.Dispose()
    }
}

$repoRoot = Resolve-RepoRoot
$configPath = Get-DemoConfigPath $repoRoot $OutputDirectory

switch ($Action) {
    "Create" {
        $credentials = New-DemoCredentials $configPath
        Write-Host "Created demo MCP WebSocket credentials at $configPath"
        Write-Host "Access token: $($credentials.AccessToken)"
        Write-Host "Secret key: $($credentials.SecretKey)"
        Write-Host "Auth token: $($credentials.AuthToken)"
        Show-RunCommand $repoRoot $configPath
    }
    "Rotate" {
        $credentials = New-DemoCredentials $configPath
        Write-Host "Rotated demo MCP WebSocket credentials at $configPath"
        Write-Host "Access token: $($credentials.AccessToken)"
        Write-Host "Secret key: $($credentials.SecretKey)"
        Write-Host "Auth token: $($credentials.AuthToken)"
        Write-Host "Restart the server for the rotated values to take effect."
        Show-RunCommand $repoRoot $configPath
    }
    "Delete" {
        if (Test-Path $configPath) {
            Remove-Item -LiteralPath $configPath
            Write-Host "Deleted demo credential file: $configPath"
        } else {
            Write-Host "Demo credential file was already absent: $configPath"
        }
        Write-Host "Restart the server without this additional config to remove these demo credentials from runtime."
    }
    "Send" {
        $credentials = Read-DemoCredentials $configPath
        $payload = New-JsonRpcPayload $Method $JsonRpc
        Write-Host "Sending to $Uri"
        Write-Host $payload
        Send-McpRequest $Uri $credentials $payload $Admin.IsPresent $UseInvalidCredentials.IsPresent
    }
    "Show" {
        Write-Host "Demo config path: $configPath"
        if (Test-Path $configPath) {
            $credentials = Read-DemoCredentials $configPath
            Write-Host "Access token: $($credentials.AccessToken)"
            Write-Host "Secret key: $($credentials.SecretKey)"
            Write-Host "Auth token: $($credentials.AuthToken)"
            Show-RunCommand $repoRoot $configPath
        } else {
            Write-Host "No demo credential file found. Run with -Action Create."
        }
    }
}
