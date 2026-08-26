# Todo

## Repository Inspection

- [x] Locate the current `ToolRequirement` model and all requirement implementations.
- [x] Locate existing source-repository clone/download logic (none found; artifact fetcher is not a Git source provisioner).
- [x] Locate existing process execution, timeout, log capture, and filesystem workspace utilities (artifact-security has a bounded runner, but it is not a provisioning abstraction).
- [x] Locate the current runtime-loader phases and determine the correct provisioning integration point (`DefaultToolRuntimeLoader#activate`, before classloading and registration).
- [x] Confirm whether any existing module already owns dependency readiness state.
- [x] Identify dependency-direction constraints before adding Maven dependencies.

## Module Creation

- [x] Create `lib/meshingress-tool-provisioning/pom.xml`.
- [x] Add `lib/meshingress-tool-provisioning` to the root reactor.
- [x] Establish `dev.mrk.meshingress.provisioning`.
- [x] Add provider-neutral request, result, state, evidence, and provider contracts.
- [x] Add provider registry and deterministic provider selection.
- [x] Add unit tests for unsupported and ambiguous provider selection; provider execution tests remain pending.

## Workspace Lifecycle

- [x] Define Git-source provisioning identity inputs: canonical identity, requested ref, and resolved commit.
- [x] Define Git-source staging and ready layout.
- [x] Prevent incomplete Git installations from being reused through a ready marker.
- [x] Add pinned-revision reuse and revision invalidation checks for Git sources.
- [x] Add cleanup behavior for failed Git staging attempts.

## Git Source Support

- [x] Consume manifest `sourceRepository` requirements through the runtime-loader adapter rather than tool-call code.
- [x] Reuse or introduce a narrow Git source-preparation boundary.
- [x] Materialize canonical source identities at `repository/vendor/<canonical-identity>`.
- [x] Record the canonical identity, requested ref, and resolved commit without persisting clone credentials.
- [x] Support optional repository subdirectory.
- [x] Verify revision changes invalidate previous provisioning identity.

## Python Provider

- [x] Detect Python project metadata and lockfiles.
- [x] Define deterministic manifest precedence for the supported pip subset.
- [x] Distinguish package projects from script-only projects.
- [x] Resolve/select the Python runtime without hard-coding one host path.
- [ ] Add `uv` availability verification (not present on this host; this slice uses pip as the local fallback).
- [x] Install dependencies into a module-local isolated environment from a disposable source copy.
- [x] Install the repository package only when appropriate.
- [x] Expose interpreter and executable locations in the provisioned resource.

## Undeclared Dependency Analysis

- [ ] Add a FawltyDeps adapter.
- [ ] Normalize undeclared dependency findings.
- [ ] Support explicit additional distributions.
- [ ] Support explicit import-to-distribution overrides.
- [ ] Refuse unapproved package-name guessing.
- [ ] Define `REPAIR_REQUIRED` behavior for ambiguous findings.

## Verification

- [ ] Support an explicit low-side-effect verification command.
- [x] Support import-based verification.
- [ ] Run dependency consistency verification.
- [ ] Bound all verification commands by timeout and output limits.
- [ ] Promote staging to ready only after verification succeeds.
- [ ] Record structured verification evidence.

## Pre-Activation Dependency Review

- [ ] During artifact assessment, materialize each immutable `sourceRepository` requirement into an assessment-scoped review workspace before approval.
- [ ] Resolve the selected Python project metadata and create an assessment-scoped candidate venv from a disposable source copy.
- [ ] Export a reviewable inventory of source revision, selected manifest/lockfile, installed distributions, interpreter/platform identity, and import/pip-check evidence.
- [ ] Keep candidate venvs separate from active runtime venvs and prevent a failed or unapproved candidate from becoming callable.
- [ ] Require activation to promote/reuse only the reviewed candidate matching the approved artifact, pinned source revision, resolver inputs, and platform fingerprint.
- [ ] Define execution isolation, network, timeout, size, and credential boundaries for pre-approval dependency installation; never run it in the ordinary server process context.

## Runtime Integration

- [x] Map required `sourceRepository` tool requirements into provisioning requests.
- [x] Reuse the published `tool-manifest.json` fetched by `RepositoryArtifactFetcher`; do not execute uploaded tool code to discover requirements.
- [x] Define the explicit runtime-cache resource handoff as the safe `cachedJar.getParent()/resources/tool-manifest.json` derivation.
- [x] Prevent a tool from becoming callable until required source resources are ready.
- [x] Attach the verified resource reference to the runtime/tool context.
- [x] Ensure ordinary tool calls never install, repair dependencies, or download models.
- [ ] Move source/Python dependency installation out of first activation and into the assessment candidate lifecycle.
- [ ] Surface broken or reprovision-required state when a ready resource later fails.

## Tests

- [x] Add local Git repository fixtures.
- [ ] Add `requirements.txt`, `pyproject.toml`, and `uv.lock` fixtures.
- [ ] Test explicit additional dependency installation.
- [ ] Test undeclared dependency evidence.
- [ ] Test ambiguous missing import refusal.
- [ ] Test installation success followed by verification failure.
- [x] Test successful pinned ready-resource reuse.
- [ ] Test invalidation on lockfile, Python version, platform, and override changes (Git revision invalidation is covered).
- [x] Run the Maven verification baseline for the affected modules and server integration.

## Documentation

- [ ] Document supported Python project formats.
- [ ] Document provisioning lifecycle states.
- [ ] Document repair policy and its security rationale.
- [ ] Document how future requirement providers are added.
- [x] Update root/current assignment handoff records after implementation begins.
