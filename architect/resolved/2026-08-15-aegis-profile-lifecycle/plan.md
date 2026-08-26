# Plan

## Portable Aegis contract

1. Add immutable profile snapshots, profile query validation, and a
   compare-and-set profile-store SPI with a synchronized in-memory reference.
2. Add a `ProfileService` that creates safe profile metadata and performs all
   mutations through a required expected revision.
3. Enforce lifecycle invariants: a revoked profile is terminal, revoking a
   profile revokes every retained credential binding, and an identity cannot be
   unlinked while bindings still reference it.
4. Add focused tests for creation, revisions, status transitions, identity
   references, binding lifecycle, and secret-safe behavior.

## Supported future MCP adapter methods

These are contract names only in this slice. They will be implemented as
privileged `@McpDispatchRoute` adapters after a host authorization and durable
store integration is approved.

| Capability | Aegis service method | Future MCP method |
|---|---|---|
| Create profile | `create` | `roles/security/profiles/create` |
| Read profile | `get` | `roles/security/profiles/get` |
| List profiles | `list` | `roles/security/profiles/list` |
| Update safe metadata | `updateMetadata` | `roles/security/profiles/update` |
| Activate / disable / revoke profile | `activate`, `disable`, `revoke` | `roles/security/profiles/activate`, `roles/security/profiles/disable`, `roles/security/profiles/revoke` |
| Link / unlink verified identity | `linkIdentity`, `unlinkIdentity` | `roles/security/profiles/identities/link`, `roles/security/profiles/identities/unlink` |
| Add / replace / revoke / detach credential binding | `bindCredential`, `replaceCredential`, `revokeCredential`, `unbindCredential` | `roles/security/profiles/credentials/bind`, `roles/security/profiles/credentials/replace`, `roles/security/profiles/credentials/revoke`, `roles/security/profiles/credentials/unbind` |

Every mutation will require `expectedRevision`; `get`, `list`, and every
mutation response will expose the resulting revision. No planned method accepts
raw tokens, client secrets, or private keys.
