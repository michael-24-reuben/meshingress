# Security and Abuse Model

## Protected Assets

- server filesystem capacity
- database capacity
- outbound network access
- private network topology
- source credentials
- stored content confidentiality
- access-token confidentiality
- tenant or principal isolation
- server availability

## Primary Threats

### Token Disclosure

A retrieval URL is a bearer capability. Disclosure can grant access until expiry or exhaustion.

Controls:

- high-entropy tokens
- TLS
- no token logging
- no referrer propagation where controllable
- `Cache-Control: no-store`
- short TTL
- bounded retrieval count
- token hash at rest
- rapid authenticated revocation

### Path Traversal and File Disclosure

Controls:

- generated opaque physical filenames
- no caller-controlled paths
- no concatenation of suggested filename into the physical path
- root containment checks
- atomic move within one dedicated filesystem
- no symlink following where avoidable
- startup permission verification

### Quota Exhaustion

Controls:

- entry ceiling
- aggregate published quota
- separate staging quota
- producer-level quotas
- concurrent ingestion limit
- cleanup before create
- staging timeout
- reservation accounting
- rejection before unbounded transfer

### Retrieval Amplification

Controls:

- request-count admission in an atomic database transaction
- active-stream ceiling
- interrupted requests remain consumed
- optional bandwidth throttling
- no range requests in v1
- hard expiry

### SSRF

Remote ingestion can target internal services, cloud metadata, loopback, or private networks.

Controls are mandatory before enabling remote URLs:

- HTTPS-only baseline
- address classification
- all-address validation
- redirect revalidation
- DNS rebinding defense
- port restrictions
- host allowlist option
- no inherited caller credentials
- strict timeouts
- actual streamed-byte enforcement

### MIME Confusion and Active Content

Controls:

- trusted MIME selection policy
- optional content sniffing
- `X-Content-Type-Options: nosniff`
- attachment disposition by default
- MIME allow/deny policy
- sanitized filename metadata
- no inline rendering by default for active formats

### Race Conditions

Relevant races:

- concurrent final retrievals
- cleanup during retrieval
- explicit deletion during retrieval
- quota reservation during concurrent creates
- publish transaction versus process failure
- metadata commit versus atomic file move

Controls:

- transactional state changes
- active-stream reference count
- pending-delete states
- idempotent cleanup
- reconciliation on startup
- same-filesystem atomic move
- failure-injection tests

## Audit Redaction

Never record:

- raw access token
- full retrieval URL
- source authorization headers
- source cookies
- URL user-info
- private query parameters
- local physical path

Audit records may include:

- storage ID
- token fingerprint prefix
- producer principal
- source host after redaction
- byte size
- MIME type
- checksum
- expiry
- lifecycle reason
- retrieval outcome
