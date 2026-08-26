# Assessment

Runtime publication installation now has a complete transactional hardening boundary for the current objective.

The original risk was that `roles/tools/installPublication` could cross several durable and runtime mutation points without an explicit rollback/reconciliation contract: repository artifact access, runtime-cache copy, runtime activation, dynamic function discovery, scope approval, active registration persistence, restart recovery, and later deletion. Earlier slices closed the activation, cache, registration, delete, and reconciliation gaps. The remaining gap was publication-record repository fetch: the runtime server still required a caller-supplied signed publication JSON even after artifact bytes could be fetched by repository API.

The final decision is to support both input modes. Inline signed publication records remain valid for tests and manual install workflows. Repository-backed installs can now provide a coordinate, causing the server to fetch the signed publication record from the repository API before applying the same existing signature, trust, revocation, checksum, scope, runtime activation, and durable registration gates.

This keeps repository and server responsibilities separate. The repository remains the source for artifact bytes and signed publication records; the runtime server remains responsible for verifying those records and controlling local runtime mutation.

Remaining policy work is separate: scanner/sandbox enforcement, richer publication eligibility policy, and any direct-registration cleanup must not be claimed by this objective.
