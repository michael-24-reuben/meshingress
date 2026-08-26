# Assessment

The Inspector already had the required live MCP schema metadata and direct
string payload path. The missing layer was a schema-to-control dispatcher;
there was no backend or workflow-definition defect to change.

The initial control set uses native browser fields. This gives numeric stepper,
calendar, time, URL, email, password, checkbox, and select behavior without a
new dependency or a parallel argument serialization model. Values that cannot
be represented safely by a specialized native field (for example a workflow
reference in a numeric field) fall back to visible text input instead of being
silently cleared or transformed.

Complex structured, uploaded, and editor-oriented schemas remain intentionally
out of scope because they need their own value, validation, and serialization
decisions.
