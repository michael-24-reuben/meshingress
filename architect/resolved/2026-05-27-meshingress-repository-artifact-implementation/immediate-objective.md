# Immediate Objective

## Next Objective

Close the repository-to-runtime acceptance gate.

The repository upload, quarantine, assess, approve, and publish MVP is now present enough to stop treating intake/review as the main missing phase. The next objective is to make the MCP runtime accept only repository-approved publication records and reject every important invalid install path.

## Current Read

Loading is mostly solved: the runtime can activate local JAR or Maven-sourced modules and expose functions through MCP.

Installation is partially solved: `roles/tools/installPublication` exists and already verifies the signed publication record, artifact checksum, installable trust status, copies the artifact into the runtime cache, activates it through `ToolRuntimeLoader`, and stores a `PUBLICATION_RECORD` registration.

Repository upload and review are MVP-concluded: `app/meshingress-repository` and the artifact model, storage, security, publication, and scope-scanner libraries exist. The repository can upload, quarantine, assess with scanner-backed `inferredScopes`, approve reviewer-selected `approvedScopes`, and publish signed records. Uploaded `requestedScopes` are preserved as claims only.

The remaining risk is policy completeness at install time. The runtime must not merely verify that a publication is signed and installable; it must prove the runtime will only honor approved scope policy from that publication and reject stale, revoked, unsigned, mismatched, or over-broad install attempts.

## Why This Comes Next

This closes the loop between the standalone Repository product and the MCP runtime.

Without this gate, the repository can review artifacts but the runtime is still too easy to bypass or under-test. After this gate is finished, the end-to-end story becomes coherent:

1. Repository ingests artifact.
2. Repository infers behavior scopes.
3. Reviewer approves final scopes.
4. Repository signs publication record.
5. Runtime installs only that approved signed record.
6. Runtime enforces only `approvedScopes`.

## Immediate Work Items

- Verify `InstallPolicyEvaluator` rejects any scope or runtime request not present in publication `approvedScopes`.
- Add or tighten runtime tests for unsigned publication records, bad signatures, checksum mismatch, revoked records, non-installable trust status, and unapproved scopes.
- Decide whether `roles/tools/installPublication` should accept a publication JSON payload only, or whether a repository client should fetch publication records from `app/meshingress-repository`.
- Add the first repository publication client only if needed for the intended operator flow; otherwise keep manual payload install as the MVP API and document that boundary.
- Ensure runtime registration persists enough provenance to answer: installed from which publication, checksum, approved scopes, trust status, and source artifact URI.
- Confirm executable checksum verification is either irrelevant for JAR-only MVP or implemented before marking runtime acceptance complete.

## Non-Goals For This Objective

- Do not integrate external scanner CLIs yet.
- Do not prioritize repository frontend polish before the runtime acceptance gate.
- Do not treat `requestedScopes` as install authority.
- Do not expand CodeQL, SpotBugs, FindSecBugs, or CycloneDX in this objective unless the runtime gate needs metadata they provide.

## Done Criteria

- `roles/tools/installPublication` rejects unsigned, invalid-signature, checksum-mismatch, revoked, non-installable, and unapproved-scope publication attempts.
- Runtime tests cover each rejection mode and the valid approved install path.
- The runtime install path records publication provenance and approved scope policy.
- The architect todo can mark runtime acceptance items as complete or split remaining scanner/UI work into separate next objectives.

## Suggested Verification

```powershell
.\mvnw.cmd -pl app/meshingress-server -am test
.\mvnw.cmd -pl app/meshingress-server -am "-Dtest=McpPublicationInstallTests" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -pl app/meshingress-repository -am test
```

## Next Objective After This

After runtime acceptance is closed, move to embedded assessment enrichment:

1. CycloneDX SBOM generation for artifact inventory metadata.
2. SpotBugs and FindSecBugs embedded Java findings.
3. More scope-rule coverage for environment, secrets, network, process, and file behavior.
4. Source-aware analysis and CodeQL query-pack generation when source/build context exists.
