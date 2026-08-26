# Context

The repository MVP currently exposes upload, assess, approve, publish, metadata, assessment, and publication endpoints. The focused tests verify the happy path and install gates, but repository HTTP API authorization and durable review/audit history are not yet implemented.

This entry intentionally groups API security, review workflow, audit events, and repository observability because they all depend on the same state-transition model and should share the SQL lifecycle/event schema from `2026-06-08-sql-tool-metadata-store`.

