# Notes

The entry remains active. Constraint schema publication and the full type-specific annotation family are implemented. The next unresolved slice is Studio constraint synchronization; backend runtime enforcement remains separate follow-up work.

## Current unresolved objective: Studio constraint synchronization

Make the Studio consume each field's compiled JSON Schema constraints and `x-mcp-*`
metadata. The intended outcome is that the matching parameter control exposes the
available bounds, patterns, defaults, and type-specific metadata in its editing and
presentation behavior, verified against `helloworld.greet`.

This slice does not add server-side request validation, cross-field validation, or
backend fallback/default resolution. Those remain follow-up work after the Studio's
schema-consumption contract is established.

## Implemented

- Added `@McpIntegerConstraints`, `@McpNumberConstraints`, `@McpArrayConstraints`, `@McpFileConstraints`, and `@McpImageConstraints` as companion annotations to `@McpInputField`.
- Added a shared compiler that emits JSON Schema bounds, defaults, array cardinality, `contentEncoding`, and `contentMediaType` while rejecting mismatched Java types at scan time.
- Kept the inheritance where Java supports it: the compiled `McpImageSchemaConstraint` extends `McpFileSchemaConstraint`. Java annotation interfaces themselves cannot inherit this way.
- Refactored generic collection schema generation so `List<String>` exposes `items: { "type": "string" }`.
- Applied rating and pagination schema constraints to `ToonverseSearchArgs`.
- Added Studio `resolveArgumentType()` as the sole type/format/enum decision point used by controls, labels, CSS type classes, and icons. It intentionally does not inspect constraint keywords.

## Verification

- `mvnw.cmd -pl lib/meshingress-tool-framework,toolspace/x-open-ink-library -am test "-Dtest=McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"` passed: 7 scanner tests.
- `npm run lint` and `npm run build` passed in `app/meshingress-studio-web`.

## Hello World schema fixture

The `helloworld.greet` tool now exposes all 25 Studio parameter object categories as schema fields. Only `name` is required. Optional values change the greeting's voice, audience, context, timing, palette, destination, and attachments without introducing runtime input enforcement.

The fixture relies on standard JSON Schema `type` and `format` metadata, with an explicit `schemaType = "null"` only where JSON Schema needs a `null` type. That metadata is consumed by the Studio type resolver; it does not introduce a frontend object-type discriminator. The old name-only greeting cache was removed because it could return a stale response when an optional modifier changed.

Verified with:

- `mvnw.cmd -pl toolspace/helloworld -am test "-Dtest=HelloWorldToolSchemaTests,McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `npm run lint`
- `npm run build`

## Full object-type constraint family

Every remaining Studio object category now has a corresponding companion annotation:
text, JSON, boolean, date, binary, null, URL, unknown, secret, enum, code, email,
decimal, clock, time, duration, interval, color, location, and geo. Existing integer,
number, array, file, and image annotations complete the full category list.

The compiler sets the related schema `format` where an annotation identifies a specific
value category. It emits standard JSON Schema keywords when available (`minLength`,
`minProperties`, `minimum`, `multipleOf`, and `default`); metadata without a JSON Schema
equivalent is deliberately namespaced under `x-mcp-*`. Annotations are scan-time schema
metadata only: they are not MCP invocation validators.

Verified with:

- `mvnw.cmd -pl lib/meshingress-tool-framework -am test "-Dtest=McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"` passed: 8 scanner tests.

## Hello World full-constraint fixture

Each optional `helloworld.greet` parameter now carries its matching companion
constraint annotation. The fixture uses bounded text, collection, numeric, temporal,
binary, file, image, decimal, and geo values along with format-specific metadata such
as URL schemes, code language, color representations, and known unknown-value types.
Only `name` remains required, and the annotations are still schema metadata rather than
runtime input enforcement.

Verified with:

- `mvnw.cmd -pl toolspace/helloworld -am test "-Dtest=HelloWorldToolSchemaTests,McpToolAnnotationScannerTests" "-Dsurefire.failIfNoSpecifiedTests=false"` passed: 8 scanner tests and 1 Hello World schema test.

## Remaining Before Resolution

- **Current:** synchronize compiled constraints into Studio controls, visible hints, defaults, and client-side editing behavior.
- **Later:** enforce compiled constraints at MCP invocation time; JSON Schema publication is currently metadata, not an authoritative request-validation gate.
- **Later:** define cross-field constraints and their error contract.
- **Later:** define backend fallback/default resolution semantics and source precedence.

## Numeric slider Studio slice (2026-08-05)

`@McpNumericSliderConstraints` now compiles to `x-mcp-control: "slider"` and
can be placed beside the existing numeric constraint. It requires the compiled
schema to supply both `minimum` and `maximum`; integers use `multipleOf` when
present and otherwise step by one, while decimals require a positive
`multipleOf`. `NumberArgumentInput` promotes only this explicit control to a
slider. This avoids changing existing counter controls or inventing a decimal
precision for a continuous numeric domain while ensuring every slider position
is valid.

The slider has visible minimum, midpoint, and maximum labels below its track.
Its right-side number field is directly editable; Enter or losing focus clamps
and snaps the entered value to the same range/step, while Escape restores the
previous value. Numeric fields without a discrete bounded range preserve the
existing counter input.

Verified with `mvnw.cmd -pl toolspace/helloworld -am test
"-Dtest=HelloWorldToolSchemaTests,McpToolAnnotationScannerTests"
"-Dsurefire.failIfNoSpecifiedTests=false"`, `npm run lint`, and `npm run
build`. The Maven run passed 8 scanner tests and the Hello World schema test,
including the two `x-mcp-control: "slider"` assertions. The live local Studio
was serving an older already-running backend schema without numeric bounds or
the control extension, so it correctly showed the counter fallback; do not
treat that preview as a visual verification of the new slider layout without
rebuilding/restarting that separate server.

## Numeric slider compact value display (2026-08-05)

The resting slider value setter now uses a three-significant-digit mantissa
with a separate suffix (`K`, `M`, `B`, `T`, `P`, or `E`), such as `999K` or
`1.23M`. The input contains only the mantissa while its unit is rendered beside
it. Hovering or focusing the setter replaces that compact display with the
exact committed value, hides the suffix, and grows the box to the raw value's
character length so editing never depends on an ambiguous abbreviation.

The setter uses a smaller 26px height and 6px radius. `npm run lint` and
`npm run build` passed in `app/meshingress-studio-web`; a targeted whitespace
check also passed. The expanded width includes two additional character cells,
so decimal points, leading zeroes, signs, and fractional digits are not clipped.

The range now has an explicit 16px thumb and 4px track. Its scale labels are
absolutely positioned at the matching minimum, midpoint, and maximum thumb
centers, avoiding the prior endpoint offset caused by flex-aligned label edges.
