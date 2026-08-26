# Context

- Existing server OIDC code validates bearer JWT issuer, audience, and expiry,
  then resolves an active Aegis profile by `issuer + subject`.
- The supplied Google client ID is deployment configuration and must not be
  inserted into tracked Studio source.
- Studio currently calls `/mcp` without `Authorization` and has no user session
  UI. Its top bar is `TopBar.tsx` and already uses a flex layout with space
  between the left brand/menu region and right-side space.
- The existing Aegis bootstrap module is portable only; it is not yet wired to
  Meshingress for first-administrator enrollment.
- The user requested native credentials be printed by the backend to prove the
  frontend path. This is deliberately replaced with redacted audit evidence,
  because logging raw credentials would create a reusable-secret disclosure.
