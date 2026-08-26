# Context

`McpInputField` currently expresses only the field name, description, and required flag. The tool annotation scanner infers simple JSON Schema types, but does not emit numeric limits, defaults, array item schemas, media information, or constraint messages.

The Studio already selects appropriate controls and icons from standard JSON Schema `type`, `format`, and `enum`. Therefore it must continue to infer parameter type from the schema rather than consume a second Meshingress-specific object-type field.

Java annotation interfaces cannot extend one another or accept a polymorphic "any constraint annotation" element. The author-facing API consequently uses companion annotations. Their compiled runtime representations can still have normal inheritance: `McpImageSchemaConstraint` extends `McpFileSchemaConstraint`.

The existing `architect/ASSIGNMENT.md` and `architect/HANDOFF.md` describe a separate active Nextcloud objective. This entry deliberately does not replace either root handoff file.
