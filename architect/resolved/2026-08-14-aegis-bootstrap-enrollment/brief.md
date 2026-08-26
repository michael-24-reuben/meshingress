# Aegis Bootstrap Administrator Enrollment

Design a reusable, framework-free Aegis bootstrap-enrollment contract for the
first administrator of a newly initialized application. The bootstrap
credential exists only until a first administrator is enrolled or the
credential expires, is revoked, or is explicitly rotated.

The result must be portable to future projects and must not wire Aegis into
Meshingress, Spring Boot, MCP routing, OIDC validation, or a concrete secret
store in this entry.

The previous Meshingress JSON-backed bootstrap-admin design is historical only.
This record supersedes its reusable concern without restoring its server-bound
authentication model.
