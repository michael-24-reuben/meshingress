# Studio authentication profile menu

**Feature ID:** `feature-studio-authentication-profile-menu-v1`  
**Recorded:** 2026-08-15T19:45:33-04:00  
**Status:** Implemented

## Feature memo

Studio has a right-aligned profile menu. It fetches a public Google web-client ID at runtime, renders the Google Identity Services button, and accepts the returned credential into the Studio’s in-memory authentication state. The signed-in display shows the Google name and email decoded by the browser, and sign-out clears that local state.

The menu also includes a native credential-submission check for development. It is disabled for unsafe non-loopback HTTP use in the browser and the server returns 404 outside development mode. The server logs only a short SHA-256 identifier fingerprint and request ID; it does not log the password or issue a native session.

## Boundary

The profile menu is a client-side sign-in surface, not persistent user provisioning. The current Google credential is retained in browser memory for subsequent requests; the server does not create or refresh a local Aegis profile from that UI callback. The native flow is a redacted development diagnostic, not native authentication.

Google button rendering additionally depends on Google’s runtime script and an Authorized JavaScript origin in the Google client configuration. The Studio build verifies the application compiles, but a successful provider popup/callback must still be confirmed in the configured browser environment.

## Evidence snapshot

- `ProfileMenu.tsx` holds the sign-in/sign-out interaction.
- `GoogleIdentityButton.tsx` is the Google Identity Services adapter.
- `AuthenticationRuntimeController.java` exposes the public browser client ID conditionally.
- `NativeLoginValidationController.java` enforces the development-only, no-session native check.

## Origin and currency

This is an implementation snapshot from source inspected and Studio-built on 2026-08-15. It is not a source of truth; verify the cited files and Google Console browser-origin configuration before relying on live sign-in behavior.
