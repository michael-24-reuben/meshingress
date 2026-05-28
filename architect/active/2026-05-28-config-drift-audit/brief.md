# Brief

Audit configuration drift between declared properties and runtime usage, then decide whether to implement missing behaviors or remove unused configuration flags.

## Scope

- `lib/meshingress-config/src/main/java/dev/mrk/meshingress/config/MeshingressProperties.java`
- Security, scope, tool registration, and secrets configuration consumers

## Constraints

- Do not remove public properties without a documented decision.
- Preserve backwards compatibility when introducing enforcement.

