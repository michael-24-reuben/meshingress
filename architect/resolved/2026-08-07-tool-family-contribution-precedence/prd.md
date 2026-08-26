# Product Requirements

## Metadata Boundary

Introduce a structured `ToolModuleMetadata` value returned by `McpToolManifestDefinition`:

```java
public record ToolModuleMetadata(
        String namespace,
        String title,
        String summary,
        String description,
        List<ToolAuthor> authors,
        String license,
        List<String> tags,
        List<ToolLink> links,
        ToolIcon icon
) {}
```

It describes the artifact module's public contribution to one tool family:

- `namespace`: lowercase, delimiter-free canonical family name;
- title, summary, description, declared authors or organization, license, tags, and public links for this module;
- optional artifact-local `ToolIcon` declaration for Studio.

Move the existing manifest-level `links()` into `ToolModuleMetadata`. Replace top-level `toolId()` with `metadata().namespace()` for native-manifest serialization.

Multiple module metadata records may use the same namespace. They must retain their own title, description, authors, links, and icon. The registry selects the family primary contribution from persisted precedence; an extension's module metadata is presented as an extension and does not overwrite the selected primary contribution's family-facing metadata.

Keep properties, external requirements, scope declarations, and README content as manifest/deployment declarations. Keep artifact coordinates, signatures, assessment results, approvals, installed locations, enabled state, and resolved secrets outside authored metadata.

Declared author information is attribution only. Repository publication ownership and signatures remain the verified provenance source.

## Tool Icon Contract

An icon is optional and artifact-local. Initial support should declare a normalized resource path, MIME type, and accessible label. It must not accept an arbitrary remote URL.

Assessment/export must validate path containment, allowed MIME types, and a bounded resource size. Studio receives a controlled served resource or retains its existing fallback icon when no tool icon is present. An extension contribution may have a contribution icon but cannot replace the root family's primary icon without a later explicit policy.

## Persisted Contribution and Precedence Model

Treat artifact identity and tool family as separate values.

Use normalized persisted records rather than a comma-separated execution column:

```text
registered_tools
  tool_id, namespace, registration state, artifact identity, ...

tool_execution_order
  namespace, tool_id, precedence, activation_mode, ...
```

The final persistence mechanism must fit the existing registration-store abstraction and its active implementations; the table names above express the required relationships, not a mandate to bypass that abstraction.

`tool_execution_order` gives a stable, administrator-controlled ascending order per namespace. A tool contribution may declare its intended family, but cannot grant itself precedence, activation, fallback status, or authority to override another function.

## Activation Modes

Define and document explicit modes rather than infer behavior from a blank or self-referential execution cell:

- `PRIMARY`: primary candidate for a family;
- `CONTRIBUTOR`: active beside the primary; lower precedence only affects duplicate function ownership;
- `REQUIRES_PRIMARY`: stays pending until an approved primary is active;
- `CONTRIBUTOR_WITH_FALLBACK`: contributes unique functions while the primary is present and may become the owner of conflicting functions if higher-precedence contributions are unavailable.

The default for a newly registered extension must be non-promoting. An extension registered before its intended primary must become a visible pending contribution unless an administrator has explicitly selected a safe activation mode and precedence.

## Function Resolution

Resolve the full active family candidate set before registering functions. Sort by persisted precedence, not discovery order. For every dotted function path, the first eligible contribution owns it.

Lower-precedence duplicate declarations must not abort the contribution or replace the owner. Skip only the duplicate and persist/report a structured warning containing the namespace, function path, owning tool ID, rejected tool ID, and time. Expose contribution state such as `ACTIVE`, `PARTIALLY_ACTIVE`, and `PENDING_PRIMARY` through the administrative surface before adding Studio presentation.

All callable function paths for a family must start with `namespace + "."`. The namespace itself remains delimiter-free.

## Non-Goals

- No automatic promotion based on artifact load order.
- No manifest-controlled overrides.
- No Java inheritance requirement between root and contributing modules.
- No arbitrary remote icon URLs, raw secret values, or unverified author claims treated as provenance.
- No backward-incompatible removal of existing registered functions without a migration plan.
