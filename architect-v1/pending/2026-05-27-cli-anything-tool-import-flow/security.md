# Security Concerns

## Main Risk: Capability Laundering

A generated CLI wrapper can make broad behavior look like a clean, narrow MCP tool. The upstream executable may read files, write files, spawn subprocesses, open network connections, or mutate local state in ways that are not obvious from the generated Java wrapper alone.
Every generated-tool import must record the exact cli-anything source provenance: repository URL, commit or release tag, generation agent, generation timestamp, and generated artifact checksum. 
This provenance record must be included in the artifact metadata uploaded to the repository, and it must be visible in the publication record for client/server verification.

The repository and server must prevent this pattern:

```txt
unreviewed generated wrapper
  → packaged as trusted Java tool
  → installed by runtime
  → exposed as MCP tool
  → performs broader actions than declared scopes imply
```

## Maven Repository Inspired Trust Bridge

The import system should use a Maven-repository-inspired design, but with stronger trust semantics.

The repository acts as the trusted bridge between:

```txt
client/server downloading installable tool artifacts
```

and:

```txt
repository-side artifact validation, review, trust assignment, checksum/signature generation, and publication
```

This is intentionally similar to a Maven artifact repository in that runtimes download versioned artifacts by coordinates or publication records. However, unlike a plain Maven repository, `meshingress-repository` must not merely store and serve JARs. It must quarantine and validate uploaded generated-tool artifacts before they become installable.

The bridge should look like:

```txt
generated artifact upload
  → repository quarantine
  → repository validation/scanning/review
  → trust status assignment
  → immutable publication record
  → checksum/signature metadata
  → client/server verified download
  → runtime install only if policy-compatible
```

## Client Download Boundary

Clients and `meshingress-server` should not install arbitrary uploaded JARs directly. They should download only approved publication records from `meshingress-repository`.

A valid install requires:

- approved trust status
- immutable publication record
- checksum match
- signature verification when signing is enabled
- compatible Meshingress runtime version
- compatible declared scopes
- non-revoked publication status

## Repository Validation Boundary

The repository should handle validation before publication, including:

- artifact structure checks
- manifest schema validation
- source provenance checks
- pinned commit verification
- generated CLI inventory
- embedded executable inventory
- SBOM/dependency analysis
- declared scope review
- smoke tests
- malware/suspicious behavior scan hooks
- human or policy-based approval

## Runtime Enforcement Boundary

The server should enforce policy at install and execution time, including:

- refusing unpublished or revoked artifacts
- refusing invalid checksums/signatures
- refusing undeclared privileged behavior where detectable
- enforcing declared scopes
- applying timeout and concurrency limits
- logging according to audit configuration
- extracting executables only into controlled runtime cache locations
- invoking executables with argv arrays, not shell-concatenated strings

## Explicit Non-Bypass Rule

`cli-anything` generation must never become a bypass around Meshingress repository trust review. Generated wrappers are useful because they accelerate integration, not because they are inherently trusted.

The security invariant is:

```txt
No generated tool artifact is installable by the official MCP runtime until the repository has produced an approved publication record and the server has verified that record.
```
