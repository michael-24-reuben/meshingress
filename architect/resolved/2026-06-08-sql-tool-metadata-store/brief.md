# Brief: SQL Tool Metadata Store

## Goal

Move Meshingress tool, registration, artifact, publication, and lifecycle metadata into a durable SQL-backed store.

## User Requirement

The store should preserve deleted tools instead of physically removing their records. Deleted tools should be retained with a lifecycle state such as `deleted`, including enough metadata to audit what existed, where it came from, and why it was removed.

## Initial Metadata Expectations

The SQL model should capture core metadata such as:

- tool name and canonical tool id;
- tool type, such as API implementation, JAR tool, Maven-backed JAR tool, native/server tool, bundle/classpath tool, etc;
- source kind and source coordinates;
- SHA-256 and other checksum metadata;
- registered functions;
- active/deleted/replaced/revoked lifecycle state;
- repository artifact and publication metadata;
- actor, request id, timestamps, provenance, trust status, scope policy, assessment summary, signature, etc.

## Scope Boundary

This entry defines the SQL-backed persistence package. It should not be implemented inside the current direct-registration hardening slice unless the active assignment is intentionally expanded.
