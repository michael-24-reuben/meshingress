# Meshingress Vision

## Identity, early adoption, and ownership

Meshingress will begin with Google sign-in for individual Gmail accounts. This
keeps early development, testing, and administrator onboarding free of a paid
domain or identity-provider subscription.

Google proves a person's identity. Meshingress/Aegis remains the authority for
local profiles, tenants, roles, grants, tool policies, credential bindings, and
audit records. A Google identity is linked by its verified issuer and stable
subject (`iss` + `sub`), never by email address alone.

The initial Google OAuth client ID is configured as deployment configuration,
not as application source. It is the expected audience for Google-issued ID
tokens.

## Future Meshingress domain

When the project is ready for public hosting, acquire and operate a
Meshingress-owned domain. Use it for the Studio origin, OAuth redirect URI,
MCP protected-resource metadata, documentation, and production service URLs.
Moving to that domain must not change Aegis profile ownership or authorization
semantics; it is a deployment identity migration, not a user-account reset.

Until then, use only explicit development or locally controlled callback URLs.
Do not register speculative production redirect URIs, and do not commit OAuth
client secrets, tokens, or credentials to the repository.
