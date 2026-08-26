# Assessment

The former manifest `toolId` mixed artifact identity with its logical tool family and did not affect runtime registration. Family identity is now `ToolModuleMetadata.namespace`, exactly one lowercase segment. Artifact identity, precedence, activation, registration state, and runtime module association remain registry-owned data.

The file-backed registration store was the compatible persistence seam. Its schema-three document retains contribution order plus the current duplicate-function decisions, so behavior is independent of module discovery order and remains administratively inspectable after restart.

## 2026-08-07 naming resolution

The public name needs three distinct ownership levels: the one-segment module
`namespace`, a one-segment `@McpTool` family, and a one-segment
`@McpFunction` leaf. The generated callable name is therefore
`<namespace>.<tool>.<function>`. Dots are delimiters only; component values
accept lower-case letters, digits, hyphens, and underscores after their first
letter.
