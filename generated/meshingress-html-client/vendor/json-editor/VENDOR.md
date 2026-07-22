# Vendor: JSON Editor

## Upstream

- Name: JSON Editor
- Package: `@json-editor/json-editor`
- Repository: `https://github.com/json-editor/json-editor`
- Description: JSON Schema based editor
- Author: Jeremy Dorn
- License: MIT

## Purpose in this project

Meshingress uses JSON Editor as a browser-side visual editor / visualizer for MCP tool input schemas.

## Attribution

JSON Editor is authored by Jeremy Dorn and maintained in the `json-editor/json-editor` GitHub repository.

The project is licensed under the MIT License. Keep the upstream `LICENSE` file with any vendored copy or substantial portion of the software.

## Local integration notes

- Vendored path: `vendor/json-editor/`
- Expected browser bundle: `dist/jsoneditor.js` or `dist/jsoneditor.min.js`
- Do not modify vendored source directly unless the change is recorded here.
- Prefer wrapping it from Meshingress UI code instead of editing upstream files.
