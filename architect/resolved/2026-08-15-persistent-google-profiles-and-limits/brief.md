# Persistent Google Profiles and Configurable Limits

## Request

Persist server-owned data for a Google-authenticated user so the same user
receives the same Meshingress privileges on a later login. Prevent a browser or
caller from spoofing identity details, selecting another user's profile, or
creating a privileged profile through client-controlled data. Rate limits,
quotas, and similar execution limits must be configurable and have an explicit
on/off control.

## Outcome Sought

A verified Google JWT establishes only an external identity. Meshingress
matches that verified identity to one durable Aegis profile using Google issuer
plus subject, then applies server-stored roles, grants, policy bindings, and
resource limits. The client never supplies a profile ID, role, grant, tenant,
or quota to obtain authority.

## Boundaries

- This is a design and planning record only; do not implement provisioning,
  session persistence, or rate limiting from this record alone.
- Do not store Google ID tokens, refresh tokens, client secrets, or raw
  credentials in an Aegis profile.
- Do not grant a newly observed Google identity access merely because its email
  resembles a known user or because the browser requests a profile.
