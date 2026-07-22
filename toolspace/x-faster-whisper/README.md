# x-faster-whisper

`fasterwhisper.transcribe` is an attached MCP tool that invokes a provisioned Python
`faster-whisper` environment through a JSON stdin/stdout bridge.

## Local provisioning

From the repository root, create or refresh the module-local virtual environment:

```powershell
pwsh .\toolspace\x-faster-whisper\scripts\Initialize-FasterWhisperVenv.ps1
```

The helper copies the vendored source to a disposable `.provisioning/staging` directory before
building it, then installs into `.venv`. It never writes into
`src/main/resources/vendor/faster-whisper`. It does not download a Whisper model.

## Runtime configuration

```properties
meshingress.faster-whisper.python-executable=toolspace/x-faster-whisper/.venv/Scripts/python.exe
meshingress.faster-whisper.model.name=small
meshingress.faster-whisper.model.device=auto
meshingress.faster-whisper.model.compute-type=default
meshingress.faster-whisper.model.local-files-only=true
meshingress.faster-whisper.generation.beam-size=5
meshingress.faster-whisper.generation.vad-filter=false
```

All upstream `WhisperModel` runtime controls and standard `transcribe` generation controls are
bound as configuration defaults. The MCP arguments can override generation controls for one call.
`model.local-files-only` must remain `true`: model acquisition is provisioning work and is never
performed by `fasterwhisper.transcribe`.

## MCP call

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "fasterwhisper.transcribe",
    "arguments": {
      "audioPath": "C:\\audio\\sample.wav",
      "language": "en",
      "beamSize": 5,
      "vadFilter": true,
      "wordTimestamps": true
    }
  }
}
```

The tool requires `FILES_READ` and `PROCESS_EXECUTE`. A model must already be present in the
configured cache or at the configured local model path.
