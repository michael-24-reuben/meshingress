# Product Requirements

## Goal

Let a Studio user authenticate with Google without navigating away from the
workflow designer, then make authenticated HTTP MCP calls. Show a compact
profile affordance on the right side of the Studio top bar.

## Google interaction

1. Load the Google Identity Services client only when a runtime client ID is
   configured.
2. A "Continue with Google" action opens the Google-managed popup.
3. Store only the short-lived Google ID token in browser memory.
4. Include that token as the HTTP `Authorization: Bearer` value for MCP calls.
5. Show the decoded non-authoritative display label only; server validation,
   issuer/audience/expiry checks, and Aegis identity lookup remain authoritative.

## Native interaction

The menu includes a development-validation native-login form. It posts its
credentials only to the dedicated backend validation endpoint over HTTPS (or a
loopback local-development connection).
The endpoint responds with a safe success/failure result and logs an audit event
containing an identifier, outcome, and request ID only. It never logs password
text, password hashes, or a bearer token.

Production native authentication remains a future capability requiring a
durable credential store, Argon2id hashing, rate limits, reset, MFA, and session
rotation. The temporary validation endpoint must be development-only.
The Studio must refuse to send the native form to a non-loopback HTTP API
endpoint; HTTPS is required outside local development.

## Acceptance criteria

- No Google client ID is committed; Studio obtains it from runtime deployment
  configuration.
- A configured Google popup token reaches `/mcp` as an authorization header.
- The profile menu is right aligned in `header.topbar` and provides sign-in,
  native validation, identity state, and sign-out controls.
- Native validation proves browser-to-backend operation without credential
  disclosure in responses or logs.
- Existing unauthenticated development behavior remains intact when Google is
  unconfigured.
