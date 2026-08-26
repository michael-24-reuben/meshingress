# Implementation Notes

## Adopted MVP policy language

The first stored policy language is deliberately grant-only. A policy applies to
one exact tool name inside one tenant, has an `ALLOW` or `DENY` effect, a
non-negative priority, and a set of required grants. Matching policies at the
highest priority are evaluated with deny-overrides. If no policy matches, the
server's existing default-deny setting controls the result.

Attribute predicates, wildcards, expression evaluation, and external PDPs are
not part of this route-enablement slice.
