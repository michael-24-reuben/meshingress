# Implementation Notes

## Active Slice

Implement the first local Python virtual-environment provider slice against the existing
`toolspace/x-faster-whisper/src/main/resources/vendor/faster-whisper` repository.

The vendor checkout remains read-only. The module-local `.venv` is the mutable provisioned
resource for this development slice. It is deliberately separate from the future managed
repository/vendor workspace that will be used after publication.

## Boundaries

- Provisioning performs dependency installation and smoke verification before tool activation.
- Normal `faster-whisper.transcribe` invocations only use the configured verified interpreter;
  they never install packages, download models, or repair the environment.
- Source-repository checkout/download and FawltyDeps integration remain follow-up phases. The
  existing vendor checkout is a local source input that lets this slice prove the provider
  boundary without mutating a client-side repository.
- Model files are distinct from Python dependencies. The bridge must default to local-only model
  loading, so a model download is never hidden inside an ordinary transcription request.

## Completed First Slice

- Added `lib/meshingress-tool-provisioning` to the reactor with provider-neutral status, result,
  evidence, resource, provider, and deterministic-selection contracts.
- Added a local Python venv provider that creates a source staging copy before invoking pip. It
  excludes `.git`, verifies declared imports, and deletes its staging copy on completion/failure.
- Provisioned `toolspace/x-faster-whisper/.venv` from the staged vendored faster-whisper project.
  Import verification reports faster-whisper `1.2.1`; `pip check` reports no broken requirements.
- Replaced the placeholder Java class with the bundled `fasterwhisper.transcribe` MCP tool and a
  packaged Python bridge. The bridge exposes the upstream model/load and generation controls as
  configuration defaults plus per-call generation overrides.
- Added the module to `meshingress-tool-bundle`; server MVC startup passed without running the
  Python bridge or model acquisition.

## Current Gaps

- The reusable provider does not yet map a declared Python source requirement through Python
  metadata/lockfile discovery into a managed virtual environment, nor attach that verified
  interpreter path to the tool runtime.
- The checked-in local helper uses pip because `uv` is not installed on this Windows host. The
  architecture still prefers a future `uv` adapter.
- No Whisper model was provisioned or transcribed. Calls intentionally require an already-local
  model and fail rather than downloading one.

## 2026-07-15 Manifest-Resolution Consolidation

The prior `2026-07-02-tool-manifest-external-requirements` architect is resolved only for its
manifest-declaration implementation. Its unmanaged portion is not complete: a source repository
requirement is currently serializable and canonically identifiable, but it is not yet converted
into a managed checkout, verified dependency runtime, or activation decision.

This record is the single active architect for that missing behavior. The integration point will
be a manifest-to-provisioning adapter in `meshingress-tool-runtime-loader`; no additional Maven
module is warranted. The provisioning module retains a provider-neutral API and therefore does
not consume static manifests directly.

## 2026-07-15 Managed Git Source Slice

`GitSourceProvisioner` now prepares trusted source requirements beneath the existing repository
layout's `vendorRoot()`, producing paths such as `repository/vendor/github.com/SYSTRAN/faster-whisper`.
It stages a clone outside the vendor root, resolves a detached commit, verifies an optional source
subdirectory, writes a non-secret ready marker, and then publishes the checkout. A pinned commit
reuses only when the marker and current Git HEAD agree; a different requested commit replaces the
managed checkout only after its replacement is ready.

The marker records canonical identity, requested ref, and resolved commit. It intentionally does
not persist clone URLs, because URLs may contain credentials. Mutable refs such as branches and
tags are fetched again instead of being silently reused.

The provider is not yet called by the runtime loader. `RuntimeToolCache` has already placed the
trusted static manifest beside a cached JAR, but the server-side loader/registration path is dirty
from unrelated work in this checkout. Keep readiness-gate integration as a separate, focused slice.

## 2026-07-15 Runtime Readiness Gate

`StaticManifestToolProvisioningGate` reads only the static manifest copied beside a cached JAR at
`<cached-jar-parent>/resources/tool-manifest.json`. For each required `sourceRepository`, it
requires an immutable 7-40 character Git commit, then invokes the managed Git provider before the
runtime loader classloads the module or registers its handlers. Missing static manifests retain
legacy no-op behavior; mutable refs fail closed before invoking Git.

The existing `ToolRuntimeLoaderConfiguration` now supplies the gate and `GitSourceProvisioner`.
No parallel loader or registry adapter was retained: the existing `RuntimeToolRegistryBridge` owns
handler registration and owner cleanup. Static manifest parsing was made tolerant of the sealed
`ToolReadme` representation by extracting portable inline README text separately before standard
metadata deserialization.

Verification passed with `mvnw.cmd -pl lib/meshingress-tool-runtime-loader -am test` (10-module
reactor), then `mvnw.cmd -pl app/meshingress-server -am -DskipTests package` (22-module reactor).
The launcher started the packaged server; `/actuator/health` returned `UP` and read-only `/mcp`
`tools/list` included `fasterwhisper.transcribe`. No tool call, registration, upload, publication,
model download, or vendor source mutation occurred.

## 2026-07-15 Tool Artifact Packaging Boundary

The x-faster-whisper tool excludes `vendor/**` and `**/.venv/**` both while copying resources and
while assembling its JAR. The second JAR-stage exclusion is necessary: an incremental Maven build
can retain files already present in `target/classes` even after a resource exclusion is introduced.
The verified package contains neither development-only path. The vendor checkout and local venv
remain available locally for development provisioning, but neither is part of a published tool.

## 2026-07-15 Faster Whisper External Manifest

`FasterWhisperManifest` is registered through the tool's auto-configuration in the same pattern
as `PowerShellCliManifest`. It declares `github.com/SYSTRAN/faster-whisper`, clone URL
`https://github.com/SYSTRAN/faster-whisper.git`, and immutable source commit
`ed9a06cd89a93e47838f564998a6c09b655d7f43`. The renamed
`FasterWhisperManifestProperties` remains the Spring Boot runtime binding and centrally supplies
the manifest's Python, model, and STT-generation property schema.

The declaration allows a published artifact's static manifest to trigger the Git readiness gate.
It does not yet create or inject the server-managed Python venv; the current local development
fallback remains only for the unuploaded workspace. The manifest unit test, focused seven-module
reactor, server executable packaging, server startup, health endpoint, and read-only tools list
all passed without invoking transcription or a publication lifecycle endpoint.

## 2026-07-15 Publication Lifecycle Validation

The explicit Faster Whisper lifecycle runner was executed against the live local server with
`FILES_READ` and `PROCESS_EXECUTE` requested and approved. The server was healthy; build, upload,
assessment, and approval completed. Publication correctly failed closed because the bytecode
scope scanner inferred four additional scopes that the initial review did not include:

- `FILES_COPY` and `FILES_MOVE`, from `FasterWhisperBridgeScript.resolve(...)`, which stages the
  packaged Python bridge into the configured runtime root.
- `TOOLS_READ` and `TOOLS_REGISTER`, from the MCP tool annotation on `FasterWhisperTool`.

The artifact is now retained at
`repository/artifacts/org/toolspace/fasterwhisper/x-faster-whisper/0.0.1-SNAPSHOT/` with the JAR,
`assessment.json`, `cyclonedx-sbom.json`, and `embedded-jar-sandbox.json`. Its `resources/`
directory exists but is empty. The uploaded JAR contains no static
`META-INF/meshingress/tool-manifest.json`; `FasterWhisperManifest` is presently registered only
when Spring loads the module. Repository assessment deliberately does not classload untrusted JAR
code, so its static metadata extractor produced an empty manifest and the exporter emitted none
of `resources/tool-manifest.json`, `resources/application.yaml`, or `resources/README.md`.

Consequently no publication, MCP installation, managed Git checkout under
`repository/vendor/github.com/SYSTRAN/faster-whisper`, or server-managed Python dependency
installation occurred. The immediate design work is to package/export the manifest as static
tool metadata before upload; that build export is recorded below. A retry still needs a fresh
review that explicitly decides all inferred scopes. The already-approved coordinate cannot be re-approved because the service accepts
approval only from `REVIEW_PENDING`; do not delete or mutate it merely to retry.

## 2026-07-15 Static Manifest Build Export

The PowerShell tool's `tool-manifest.json` is checked-in data; its Maven build does not generate
that file. To avoid a second hand-maintained Faster Whisper JSON document, the manifest API now
provides `McpToolManifestExecutor`. It instantiates a no-argument
`McpToolManifestDefinition` by class name and writes its portable native metadata JSON to a
specified output path.

`x-faster-whisper` binds this executor through `exec-maven-plugin` at `process-classes`, after
the manifest class compiles and before `maven-jar-plugin` packages `target/classes`. It writes
`META-INF/meshingress/tool-manifest.json`, so repository assessment can safely extract the
pinned Git requirement without classloading the uploaded JAR. Focused tests cover executor
serialization and class validation, the tool test verifies the generated static manifest, and a
package inspection confirmed the final JAR contains that entry with 41 properties and the
source-repository requirement.

## 2026-07-15 External Source Publication Lifecycle

The lifecycle runner completed a fresh upload, assessment, approval, publication, and runtime
installation using coordinate `org.toolspace.fasterwhisper:x-faster-whisper:0.0.1-SNAPSHOT-rerun-20260715-v4`.
The repository exporter created `resources/application.yaml`, `resources/README.md`, and
`resources/tool-manifest.json`. The runtime readiness gate then materialized the required source
at `repository/vendor/github.com/SYSTRAN/faster-whisper`, wrote its ready marker, and verified
commit `ed9a06cd89a93e47838f564998a6c09b655d7f43`. `tools/list` reports exactly one
`fasterwhisper.transcribe` handler.

Two provisioning lifecycle defects were corrected while exercising this path:

- `GitSourceProvisioner` applies a process-local Git `safe.directory` override to managed
  checkout commands, using Git's forward-slash path form. It does not modify global Git config.
  Its staging deletion now closes the directory walk before delete attempts, which avoids a
  Windows handle leak.
- `Invoke-FasterWhisperLifecycle.ps1` serializes nested signed publication records at depth 100
  and safely tests for a JSON-RPC error property under StrictMode. It submits the publication
  returned by publish, allowing local repository installation without an external
  `repository.api-base-url`.

The server bundle no longer directly depends on `x-faster-whisper`; otherwise the preloaded
handler collides with the uploaded runtime module. The tool remains a normal Maven reactor module
but is now installed only through this publication lifecycle.

No server-managed Python virtual environment was created: the vendor source is ready, while
manifest-to-Python metadata/lockfile resolution and runtime interpreter injection remain the
next active slice. No model was downloaded and the vendored client-side source under
`toolspace/x-faster-whisper/src/main/resources/vendor/faster-whisper` was not changed.

## 2026-07-15 Exported Application Properties Format

The native metadata exporter now emits `resources/application.properties`, not YAML. It converts
declared `boolean`, `long`/`integer`, `number`, `integer-list`, and `number-list` defaults into
unquoted property values before writing. String-like defaults remain safely escaped for Java
properties syntax. An incompatible non-empty typed default fails export rather than producing a
misleading string value.

The metadata reader, repository resource allowlist, remote resource fetcher, fixture tests, and
PowerShell manifest documentation now use `application.properties`. This change affects future
assessments/publications; it intentionally does not rewrite the already-published v4 resource.
No test command was run at the user's direction.

## 2026-07-15 Server-Managed Python Resolution

The static-manifest readiness gate now recognizes a required source as a Python environment only
when the manifest declares exactly one `*.python-executable` property. It discovers the supported
pip project surface without executing source code: `pyproject.toml`, `setup.py`, and `setup.cfg`
are package-project metadata; a script-only project may use `requirements.txt`; `uv.lock`,
`poetry.lock`, and `Pipfile.lock` are recorded deterministically but are not consumed until their
respective resolver is implemented. Package projects are installed from the disposable staging
copy; script projects install their selected requirements file.

For Faster Whisper, the gate derives the read-only source from
`repository/vendor/github.com/SYSTRAN/faster-whisper` and targets the isolated interpreter at
`repository/runtime/python/github.com/SYSTRAN/faster-whisper/<pinned-commit>/.venv`. The
interpreter path is passed through a highest-precedence property source into only the tool child
context as `meshingress.faster-whisper.python-executable`; no JVM-global property or vendor write
is used. Bootstrap Python resolves first from `MESHINGRESS_PROVISIONING_PYTHON_EXECUTABLE`, then
`PYTHON`, then the host `python` command.

Focused validation passed twice after the Windows fix:
`mvnw.cmd -pl lib/meshingress-tool-runtime-loader -am test` (6 provisioning tests and 5 runtime
loader tests). The full server reactor compiled the new provider configuration. It contained one
pre-existing/unrelated registration fixture failure: its checksum-mismatch test instead received
`TOOL_REGISTRATION_LOCAL_JAR_NOT_FOUND` because the expected sample JAR was absent.

Live startup reconciliation exercised the real managed source and reached `PythonVenvProvisioner`.
The first attempt failed closed before creation because the request identity contained `:` and was
used as a Windows staging-directory segment. `PythonVenvProvisioner` now sanitizes that disposable
segment; no vendor file was written. A second end-to-end activation could not be triggered because
the saved dynamic registration was no longer reconcilable and a fresh lifecycle install was
rejected by `PublicationRecordVerifier` with `Publication record signature is invalid`. That
publication-signature defect is outside the Python provider; a valid runtime installation is still
needed to observe the final pip install and import smoke check in the server-managed venv.

## 2026-07-15 Pre-Activation Dependency Review Decision

The v5 lifecycle proved that the static manifest can declare the Faster Whisper Git requirement,
but it did not invoke Git or Python provisioning: `PublicationRecordVerifier` rejected the
publication record before runtime installation. The existing runtime gate therefore has the wrong
approval boundary for dependency review: source checkout and `pip install` are deferred until
activation, where the user cannot inspect their resulting contents beforehand.

The required replacement lifecycle is assessment-scoped candidate resolution. Assessment first
materializes the pinned source in a review workspace, discovers its supported Python metadata,
creates a candidate venv from a disposable source copy, verifies imports and `pip check`, and
exports the source revision plus installed-distribution inventory. The approver reviews that
evidence alongside existing JAR/SBOM/scope evidence. Publication/activation then promote or reuse
only that exact candidate; no new source download or Python installation is allowed at activation.

This cannot reuse the current host-process `PythonVenvProvisioner` unchanged for untrusted input:
installing a project can execute build hooks. The candidate stage needs an explicit constrained
runner/workspace policy before it is connected to `ArtifactService.assess(...)`.
