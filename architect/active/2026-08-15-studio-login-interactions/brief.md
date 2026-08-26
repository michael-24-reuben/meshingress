# Studio Google and Native Login Interactions

Implement a Google-first Studio login interaction using Google Identity Services
popup sign-in and connect the resulting verified identity to existing MCP HTTP
authorization. Add a right-aligned profile menu to `header.topbar`.

Document and expose a development-only native-login interaction so the frontend
can prove a native submission reached the backend. The backend must never print
or persist raw passwords, bearer tokens, or other credential material; it may
record only a redacted audit event and return safe confirmation metadata.

This work does not replace Google with native login, auto-promote the first
Google account, or deliver password reset/MFA/account recovery.
