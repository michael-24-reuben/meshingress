# Todo

- [ ] Define runtime `tools/` directory layout.
- [ ] Define dynamic tool manifest schema.
- [ ] Add `ENV_READ` to `McpToolScope` or document why `CONFIG_READ` is sufficient.
- [ ] Define runtime environment variable model for installed tools.
- [ ] Design install/enable/disable/uninstall lifecycle.
- [ ] Design Meshingress artifact registry web pages.
- [ ] Design Maven-compatible registry endpoint layout.
- [ ] Design JSON registry API endpoints.
- [ ] Define scaffold modes: static module vs dynamic runtime module.
- [ ] Define checksum/signature verification requirements.
- [ ] Define compatibility metadata between runtime, API library, annotation library, and tool artifacts.


## End-Phase Security Verification

- [ ] Define JAR scope integrity scanner requirements.
- [ ] Select bytecode scanner library candidate such as ASM, ClassGraph, Javassist, or Byte Buddy.
- [ ] Define API/package/method patterns that imply each scope.
- [ ] Define risk verdicts: `PASS`, `WARN`, `FAIL`, `BLOCK`.
- [ ] Define how AI-assisted review receives scanner findings and produces non-authoritative summaries.
- [ ] Define install gate behavior for missing scopes, dangerous APIs, unsigned artifacts, and prohibited patterns.
- [ ] Persist scope integrity reports with installed tool metadata.
- [ ] Mark scope integrity verification as end-phase/post-MVP until runtime install and registry workflows are stable.
