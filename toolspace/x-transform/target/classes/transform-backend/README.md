# Transform backend

This is the non-UI runtime extraction for `x-transform`. It is a JSON-lines process, not an HTTP server: Meshingress starts it for one `transform` call and writes the selected type, source text, optional secondary text, and optional settings to standard input.

Install its declared converter dependencies before using the tool locally:

```powershell
Set-Location toolspace/x-transform/src/main/resources/transform-backend
npm install --ignore-scripts
```

`node_modules` is intentionally excluded from Git and Maven resources. No Next, React, pages, components, assets, styles, or browser worker wrapper are included here.
