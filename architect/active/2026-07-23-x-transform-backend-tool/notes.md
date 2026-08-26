# Notes

- The public MCP shape is one tool named `x-transform`, with functions `transform` and `list-types`; individual conversions are enum values passed to `transform.type`.
- `TransformTool` emits `CodeContent`, whose wire kind is `text.code` and whose `language` is supplied by `TransformType`.
- The local backend dependencies are deliberately excluded from Maven resources and Git. Provisioning is `npm install --ignore-scripts` in `toolspace/x-transform/src/main/resources/transform-backend`.
- Focused module verification passed after the code-content implementation and bridge wiring.
