# Meshingress PowerShell CLI Tool

Adds a Meshingress MCP tool named `cli.powershell` with function `execute`.

## Behavior

- Receives a PowerShell script body.
- Writes it to a temporary `.ps1` file.
- Executes it with `pwsh -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File <temp.ps1>`.
- Captures stdout and stderr concurrently.
- Appends each execution track to `DispatchExecutionResult.content`.
- Returns full ordered tracks in `structuredContent.tracks`.
- Marks `error=true` when the process exits non-zero, times out, or cannot start.

## Add to root `pom.xml`

```xml
<module>toolspace/powershell-cli</module>
```

## Add to `app/meshingress-server/pom.xml`

```xml
<dependency>
    <groupId>dev.mrk.toolspace</groupId>
    <artifactId>powershell-cli</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Example call payload

```json
{
  "script": "Write-Output 'hello world'; Write-Error 'sample error'",
  "timeoutMs": 20000,
  "executable": "pwsh"
}
```
