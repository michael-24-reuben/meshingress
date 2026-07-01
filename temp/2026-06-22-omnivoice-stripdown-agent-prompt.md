# OmniVoice Stripdown Agent Prompt

**Architect Entry:** `2026-06-22-omnivoice-stripdown-java-wrapper`  
**Prompt File:** `omnivoice-stripdown-agent-prompt.md`  
**Status:** active  
**Created:** 2026-06-22  
**Owner:** Architect-handling agent  
**Delegated Worker:** OmniVoice stripdown sub-agent

---

## Purpose

This file is the reusable prompt for the delegated agent responsible for inspecting the OmniVoice GitHub repository, reducing it to an inference-focused runtime design, and producing a wrapper handoff package for the main Meshingress implementation flow.

The delegated agent does **not** implement the final Java/Meshingress module. It prepares the technical design, runtime bill of materials, stripdown plan, and wrapper contract that the main implementation agent will consume.

---

# Prompt To Give The Stripdown Agent

You are the **OmniVoice Stripdown Sub-Agent**.

Your task is to inspect the OmniVoice GitHub repository and produce a focused implementation handoff package for a Java/Meshingress integration.

The integration goal is not to port OmniVoice directly into Java. The goal is to keep OmniVoice inside a controlled Python runtime boundary and expose a stable wrapper interface that Java can invoke through either a subprocess or a local resident service.

You are working under an architect-led flow. Your output will be consumed by the main implementation agent, which will later build the Meshingress-facing Java tool module.

---

## Primary Objective

Create a complete stripdown and wrapper design for OmniVoice.

You must determine:

1. what files/components are required for inference,
2. what dependencies are required at runtime,
3. what model assets are required or optional,
4. what components can be removed or ignored,
5. how to expose OmniVoice through a stable CLI wrapper,
6. how to expose OmniVoice through a future resident service wrapper,
7. how voice cloning, voice design, and model management should be represented,
8. how downloadable/importable voice assets should be represented through provider adapters,
9. what risks, gaps, and unresolved implementation issues remain.

---

## Context

The parent system is **Meshingress**, a Java/Spring Boot MCP tool runtime.

The final Java integration will likely live under:

```txt
toolspace/omnivoice-tts/
```

The final tool will expose functions such as:

```txt
generate
design
clone
models.status
models.pull
voices.import
voices.providers
voices.download
runtime.health
```

Your responsibility is to design the Python-side runtime boundary and produce handoff artifacts. Do not write the final Java module unless explicitly asked by the main agent later.

---

## Repository Inspection Requirements

Inspect the OmniVoice repository and identify the following categories.

### Required Runtime Components

Find and list the files/modules needed for:

- loading the OmniVoice model,
- loading tokenizer/audio tokenizer components,
- generating speech from text,
- generating speech from a voice instruction/design prompt,
- cloning voice from a reference audio sample,
- optionally transcribing reference audio if reference text is absent,
- writing generated audio to disk,
- exposing existing CLI behavior if present.

### Optional Runtime Components

Find and list components that are useful but not required for minimum inference:

- demo UI,
- web UI,
- Gradio app,
- examples,
- benchmark scripts,
- notebooks,
- development utilities,
- evaluation scripts,
- training/fine-tuning code,
- packaging-only files.

### Removable / Excludable Components

List components that can be removed from a stripped runtime package without breaking inference.

For each removable component, explain why it is safe to remove or isolate.

### Dependency Classification

Classify dependencies into:

```txt
required-runtime
optional-runtime
dev-only
demo-only
training-only
unknown-needs-verification
```

For each dependency, provide:

- package name,
- purpose,
- required/optional status,
- where it is used,
- whether it is safe to remove from MVP.

---

## Model Asset Requirements

Identify all model assets used by OmniVoice.

At minimum, determine:

- primary OmniVoice model identifier,
- audio tokenizer model identifier,
- optional ASR model identifier for auto reference transcription,
- where each model is downloaded from,
- whether each model is required for basic TTS,
- whether each model is required for voice cloning,
- whether each model is required only when reference text is missing,
- expected cache directory behavior,
- whether offline/pre-pulled installation is possible,
- whether automatic download can be disabled or controlled.

Produce a model matrix like this:

```md
| Model | Provider | Required For | Optional? | Download Trigger | Cache Path | Notes |
|---|---|---|---|---|---|---|
```

---

## Feature Matrix

Produce a feature matrix with these columns:

```md
| Feature | Supported By Repo | MVP Support | Requires Extra Model | Requires Network | Risk Level | Notes |
|---|---:|---:|---:|---:|---|---|
```

Include at least:

- basic TTS,
- instructed voice design,
- voice cloning with reference audio and reference text,
- voice cloning with reference audio and automatic transcription,
- model prefetch,
- model status,
- local voice asset import,
- provider-based voice download/import,
- resident service mode,
- streaming output,
- batch generation.

---

## Wrapper Design Requirements

Design two wrapper modes.

---

### Mode A: CLI Wrapper

Design a stable CLI interface for one-shot calls.

The wrapper should accept a JSON request file or JSON stdin and return JSON stdout.

Recommended command shape:

```bash
python wrapper_cli.py --request request.json
```

or:

```bash
python wrapper_cli.py --stdin
```

The wrapper must:

- load inputs from JSON,
- validate operation type,
- invoke OmniVoice,
- write generated audio to the requested output path,
- return a structured JSON result on stdout,
- emit logs on stderr,
- return stable exit codes,
- never mix human logs into JSON stdout.

Define the request schema for:

```txt
generate
design
clone
models.status
models.pull
voices.import
voices.download
runtime.health
```

Define the response schema for success and failure.

Example success response:

```json
{
  "status": "ok",
  "operation": "clone",
  "model": "k2-fsa/OmniVoice",
  "outputPath": "/tmp/out.wav",
  "sampleRate": 24000,
  "durationMs": 3210,
  "warnings": []
}
```

Example failure response:

```json
{
  "status": "error",
  "operation": "clone",
  "errorCode": "REFERENCE_AUDIO_MISSING",
  "errorMessage": "Reference audio path does not exist.",
  "details": {
    "refAudioPath": "/tmp/ref.wav"
  }
}
```

---

### Mode B: Resident Service Wrapper

Design a future service mode where the model can stay loaded.

The service may use HTTP, gRPC, or local socket. Prefer simple local HTTP unless there is a strong reason not to.

Required endpoints:

```txt
GET  /health
GET  /runtime/status
GET  /models/status
POST /models/pull
POST /tts/generate
POST /tts/design
POST /tts/clone
GET  /voices/providers
POST /voices/import
POST /voices/download
```

The resident service must support:

- model warm loading,
- readiness reporting,
- bounded concurrency,
- explicit output paths or managed output directories,
- structured errors,
- graceful shutdown notes,
- no unauthenticated remote exposure by default.

---

## Voice Asset and Provider Design

The user wants support for downloading or importing voices from supported platforms/providers.

Do **not** hard-code unsupported providers or claim provider support without verifying it.

Instead, design a provider abstraction that can support future adapters.

### Required Provider Abstraction

Define a provider interface/concept with at least:

```txt
id
displayName
enabled
requiresAuth
capabilities
listVoices/download/import behavior
terms/policy notes
```

Suggested conceptual interface:

```python
class VoiceProvider:
    def id(self) -> str: ...
    def display_name(self) -> str: ...
    def capabilities(self) -> dict: ...
    def list_voices(self, query: dict) -> dict: ...
    def download_voice(self, request: dict) -> dict: ...
    def import_reference(self, request: dict) -> dict: ...
```

### Voice Asset Store

Design a local voice asset store layout.

Suggested layout:

```txt
voice-assets/
├─ index.json
├─ providers/
│  └─ <provider-id>/
│     └─ <voice-id>/
│        ├─ voice.json
│        ├─ reference.wav
│        ├─ preview.wav
│        └─ metadata.json
└─ local/
   └─ <voice-id>/
      ├─ voice.json
      ├─ reference.wav
      └─ metadata.json
```

Each voice asset metadata record should include:

```json
{
  "voiceId": "string",
  "source": "local | provider",
  "providerId": "string|null",
  "displayName": "string",
  "referenceAudioPath": "string",
  "referenceText": "string|null",
  "language": "string|null",
  "tags": [],
  "createdAt": "ISO-8601",
  "license": "string|null",
  "termsUrl": "string|null",
  "usageRestrictions": []
}
```

### Policy Requirement

Provider-based download/import must be disabled by default unless explicitly enabled by configuration.

Document:

- policy risk,
- provider terms risk,
- voice cloning misuse risk,
- authentication/secrets risk,
- audit requirements.

---

## Security and Abuse Boundaries

Voice cloning and provider-based voice acquisition are sensitive.

Your design must include safeguards:

- no default remote exposure of resident service,
- no provider download enabled by default,
- all provider imports audited,
- generated audio output paths constrained by configuration,
- reference audio reads constrained by configuration,
- no raw secrets in logs,
- no arbitrary shell command construction,
- no mixing stderr logs into stdout JSON,
- ability to disable auto-download of models if possible,
- clear distinction between local user-provided reference audio and third-party downloaded voice assets.

Do not design features intended to impersonate people without permission or bypass provider restrictions.

---

## Handoff Package Requirements

Produce the following output files.

```txt
omnivoice-handoff/
├─ manifest.json
├─ runtime-bom.md
├─ stripdown-plan.md
├─ wrapper-contract.md
├─ service-contract.md
├─ model-matrix.md
├─ feature-matrix.md
├─ provider-contract.md
├─ voice-asset-store.md
├─ install-notes.md
├─ security-notes.md
├─ risks.md
└─ recommended-next-steps.md
```

---

## Required File Details

### `manifest.json`

Must include:

```json
{
  "name": "omnivoice-runtime-handoff",
  "createdAt": "ISO-8601",
  "repo": {
    "url": "string",
    "commit": "string|null",
    "branch": "string|null"
  },
  "status": "complete|partial|blocked",
  "runtimeModes": ["cli", "service"],
  "features": {},
  "models": [],
  "knownRisks": [],
  "nextSteps": []
}
```

### `runtime-bom.md`

Must include:

- required Python version,
- required runtime packages,
- optional packages,
- removable packages,
- system dependencies,
- GPU/CUDA notes,
- CPU fallback notes,
- disk/cache notes.

### `stripdown-plan.md`

Must include:

- files to keep,
- files to remove,
- files to isolate,
- recommended runtime directory layout,
- Docker/runtime packaging notes.

### `wrapper-contract.md`

Must include:

- CLI command syntax,
- request schemas,
- response schemas,
- exit codes,
- error codes,
- stdout/stderr contract,
- examples for `generate`, `design`, and `clone`.

### `service-contract.md`

Must include:

- service endpoints,
- request/response bodies,
- lifecycle behavior,
- health/readiness behavior,
- concurrency notes,
- security defaults.

### `provider-contract.md`

Must include:

- provider interface,
- provider configuration schema,
- local asset normalization behavior,
- disabled-by-default policy,
- unsupported/unknown provider handling.

### `risks.md`

Must include:

- technical risks,
- model download risks,
- dependency risks,
- legal/policy risks,
- performance risks,
- unknowns that require manual verification.

---

## Output Quality Rules

- Be precise.
- Do not invent unsupported repo features.
- Cite the exact file paths or code locations you inspected.
- Separate verified facts from assumptions.
- Mark unresolved questions clearly.
- Avoid vague advice.
- Produce implementation-ready contracts.
- Prefer deterministic install/runtime behavior over auto-magic behavior.
- Keep the Java side independent from internal Python repo layout.

---

## Final Response Format

When complete, return:

```txt
STATUS: complete | partial | blocked

HANDOFF_DIRECTORY: omnivoice-handoff/

SUMMARY:
<short summary>

KEY_DECISIONS:
- ...

BLOCKERS:
- ...

NEXT_STEPS_FOR_MAIN_AGENT:
- ...
```

Do not include the full handoff contents inline if files have been produced. Point to the generated handoff directory and summarize the contents.

---

# Architect Notes For Parent Agent

The parent architect-handling agent should attach this file to the active architect entry:

```txt
architect/active/2026-06-22-omnivoice-stripdown-java-wrapper/omnivoice-stripdown-agent-prompt.md
```

The parent agent should use this prompt to launch the sub-agent responsible for repository stripdown and wrapper design.

The main Meshingress implementation should not begin until the sub-agent has returned at least:

- `manifest.json`,
- `runtime-bom.md`,
- `wrapper-contract.md`,
- `model-matrix.md`,
- `feature-matrix.md`,
- `risks.md`.

Provider-based voice download/import should remain disabled by default unless an explicit provider policy and terms-aware implementation are accepted.
