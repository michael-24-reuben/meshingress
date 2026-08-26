# PRD: SQL Tool Metadata Store

## Problem

Meshingress currently has useful runtime and repository metadata models, but part of the live tool registration surface is in-memory. That means registered tool metadata can disappear across process restarts and deleted/replaced tools are not preserved as durable lifecycle records.

## Product Outcome

Meshingress should have a SQL-backed metadata store that can answer:

- What tools exist now?
- What tools existed before?
- Which tools were deleted, replaced, revoked, or disabled?
- What artifact or source produced a tool?
- What checksum, publication, assessment, and scope data supported installation?
- Which functions were exposed by each tool registration?
- Who performed lifecycle actions and when?

## Initial SQL Entity Candidates

These are starting rows/tables or logical entities, not a final schema:

| Entity | Purpose | Candidate fields |
|---|---|---|
| `tools` | Durable tool catalog. | `tool_id`, `display_name`, `tool_type`, `current_state`, `created_at`, `updated_at`, `deleted_at`, `etc` |
| `tool_registrations` | History of registration attempts and active/replaced/deleted records. | `registration_id`, `tool_id`, `phase`, `source_kind`, `status`, `actor`, `request_id`, `registered_at`, `replaced_registration_id`, `runtime_module_id`, `etc` |
| `tool_sources` | Normalized source metadata for local JAR, Maven, API/native, bundle, repository publication, etc. | `source_id`, `tool_id`, `source_kind`, `path`, `group_id`, `artifact_id`, `version`, `publication_id`, `repository_uri`, `etc` |
| `tool_checksums` | Checksums for artifacts, local JARs, extracted files, and publication payloads. | `checksum_id`, `tool_id`, `source_id`, `algorithm`, `value`, `verified_at`, `verification_status`, `etc` |
| `tool_functions` | Functions exposed by a registration or publication. | `function_id`, `tool_id`, `registration_id`, `name`, `title`, `version`, `enabled`, `visibility`, `input_schema_json`, `etc` |
| `tool_artifacts` | Repository artifact records. | `artifact_id`, `tool_id`, `group_id`, `artifact_name`, `version`, `packaging`, `artifact_uri`, `trust_status`, `artifact_checksum_id`, `etc` |
| `tool_artifact_files` | Extracted file inventory. | `file_id`, `artifact_id`, `path`, `size`, `checksum_id`, `executable`, `etc` |
| `tool_scope_policies` | Requested, inferred, approved, and denied scope state. | `scope_policy_id`, `tool_id`, `artifact_id`, `requested_json`, `inferred_json`, `approved_json`, `denied_json`, `etc` |
| `tool_assessments` | Scanner and SBOM assessment summaries. | `assessment_id`, `artifact_id`, `status`, `scanner_count`, `finding_count`, `summary_json`, `raw_report_uri`, `etc` |
| `tool_publications` | Signed publication records and installable trust metadata. | `publication_id`, `artifact_id`, `trust_status`, `artifact_uri`, `artifact_checksum_id`, `scope_policy_id`, `assessment_id`, `provenance_json`, `revoked`, `published_at`, `signature_algorithm`, `signature`, `etc` |
| `tool_lifecycle_events` | Append-only audit trail. | `event_id`, `tool_id`, `registration_id`, `artifact_id`, `event_type`, `from_state`, `to_state`, `actor`, `request_id`, `reason`, `created_at`, `etc` |

## Tool Type Examples

`tool_type` should be extensible. Initial values may include:

- `api-implementation`;
- `jar-tool`;
- `maven-jar-tool`;
- `native-server-tool`;
- `classpath-bundle-tool`;
- `repository-publication-tool`;
- `external-service-tool`;
- `etc`.

## Lifecycle State Examples

State should be explicit and queryable. Initial values may include:

- `active`;
- `inactive`;
- `pending-review`;
- `approved`;
- `rejected`;
- `replaced`;
- `deleted`;
- `revoked`;
- `failed`;
- `etc`.

## Non-Goals

- Do not remove existing runtime registry behavior before an adapter/migration plan exists.
- Do not collapse requested, inferred, approved, and denied scopes into a single trusted field.
- Do not make direct registration the production trust path merely because it has durable rows.
- Do not physically delete tool metadata as the default deletion behavior.
