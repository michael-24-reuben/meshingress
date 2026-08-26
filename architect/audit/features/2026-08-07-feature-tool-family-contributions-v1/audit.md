# Tool-family contributions and persisted precedence

**Feature ID:** `feature-tool-family-contributions-v1`  
**Recorded:** 2026-08-07T14:39:03Z  
**Status:** Implemented

## Feature memo

Separate tool artifacts can declare the same common namespace and contribute functions to one logical tool family. The registration system determines function ownership from persisted precedence, rather than module discovery order. A lower-precedence contribution does not replace an existing function owner, but its unique functions remain usable as extensions.

The example below is deliberately abbreviated. It captures the separation between a module-authored namespace and the server-side precedence resolver:

```java
public record ToolModuleMetadata(
        String namespace,
        String title,
        ...
) { ... }

for (ContributionFunctions candidate : candidates.stream()
        .sorted(comparingInt(value -> value.contribution().precedence()))) {
    owners.putIfAbsent(function.name(), owner);
}
```

## Evidence snapshot

- `ToolModuleMetadata.java` lines 5-20 defines the module-authored namespace as the common logical family identifier.
- `ToolContributionResolver.java` lines 11-29 orders contributions by persisted precedence and captures duplicate-function conflicts without overwriting the earlier owner.
- The linked verification record covers contribution order, primary/extension behavior, conflicts, fallback, and restart persistence.

## Origin and currency

This memo was recorded from the resolved `2026-08-07-tool-family-contribution-precedence` architecture record and a source inspection on 2026-08-07. It is not a source of truth: confirm the references in `audit.json` before depending on the behavior, because the implementation can change after this snapshot.
