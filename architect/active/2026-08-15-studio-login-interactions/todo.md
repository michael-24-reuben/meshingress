# Todo

- [x] Add runtime Google client-ID configuration and in-memory Studio auth state.
- [x] Add right-aligned profile menu and Google popup interaction.
- [x] Attach a Google ID token to HTTP MCP requests.
- [x] Add development-only native-login validation endpoint with redacted audit logging.
- [x] Add native-login form and safe result display.
- [x] Verify server and Studio builds.
- [ ] Manually test Google login after the Studio origin is authorized in Google Cloud and the configured server OIDC audience is available to Studio (or a deployed runtime override supplies the client ID).
- [ ] Wire explicit first-administrator bootstrap enrollment before using privileged security-management routes.
- [ ] Keep WebSocket authenticated handoff as a follow-up to the OIDC migration entry.
