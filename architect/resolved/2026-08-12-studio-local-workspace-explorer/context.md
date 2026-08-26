# Context

- Browser folder selection is implemented through the File System Access API where available, with directory-enabled input fallback.
- Browser privacy does not expose an absolute native filesystem path through the fallback; the UI uses the selected root label and relative file paths.
- Recent roots and independently selected files are cached only in browser local storage. File handles and contents are not persisted or sent to Meshingress.
