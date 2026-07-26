param(
    # `OpenAPI` -> Open the API documentation.
    # `README` -> Open the README file.
    # `LICENSE` -> Open the LICENSE file.
    # `CHANGELOG` -> Open the CHANGELOG file.
    [ValidateSet("OpenAPI", "README", "LICENSE", "CHANGELOG")]
    [string]$Docs = "README",

    [string]$ServerAddress = "",

    [ValidateRange(0, 65535)]
    [int]$ServerPort = 0
)

& (Join-Path $PSScriptRoot "..\private\Audits.ps1") @PSBoundParameters
