# Implementation Plan

1. Extend Studio runtime configuration with an optional Google client ID,
   fall back to the public configured server OIDC audience, and add an
   in-memory authentication state used by API calls.
2. Add the Google Identity Services popup adapter, a profile menu in the
   right-hand top bar, and a development-only native validation form.
3. Add a narrow server endpoint for native validation/auditing, restricted to
   `meshingress.security.mode=dev`; never log supplied secret values.
4. Preserve the existing OIDC resource-server implementation for Google token
   verification and Aegis profile resolution. Do not create an OIDC redirect or
   store a Google refresh token in this direct-popup slice.
5. Verify the Studio build/lint and server tests; Google popup requires a real
   browser configuration and is manually verified after the owner adds the
   Studio origin in Google Cloud.

## Future native-login production implementation

- Credential binding backed by an external secret store and Argon2id verifier.
- Login throttling, lockout audit, reset/recovery, MFA, and own-session
  rotation.
- Same profile/session abstraction as Google, so MCP does not care whether the
  user signed in natively or through Google.
