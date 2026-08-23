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

## Workflow run context and entry points

A workflow may contain zero or one **Flow Start** node. When present, Flow
Start is the run gateway: it declares and initializes an explicit runtime
context, analogous to an executable program's IDE run configuration. The
context can hold runtime properties such as environment-style variables and
launch inputs. It is defined by the workflow, rather than inherited implicitly
from the host process environment.

Flow Start is optional. A workflow without it has no initialized runtime
context; a node that attempts to read runtime context receives `undefined`.
Within an initialized context, an omitted key is distinct from an explicitly
present `null` value. This distinction must remain visible to expressions,
nodes, and run diagnostics.

Flow Start is the sole entry point when it exists. Without it, the runtime
infers a single graph root: a node with no incoming edge. Workflows with
multiple roots are ambiguous and must be rejected or declare an explicit entry
node. The storage order of `edges` must not affect execution; downstream work
is scheduled from graph dependencies in a deterministic order.

The Flow Start context is an immutable baseline for a run. A future
runtime-context manipulation node may create a derived context snapshot for
its downstream path, rather than mutating shared context in place. That keeps
parallel branches reproducible and prevents context races.

## Project-local remembered profiles

Each local project may keep non-authoritative, project-local remembered-profile
records under `./.meshingress/`. These records help the Studio recognize
accounts previously used with that project; they are not credentials and grant
neither authentication nor authorization. They may retain a provider, verified
identity reference (`issuer` + `subject`), user-facing display metadata, and
local state such as last used or benched. Because even account labels are
personal data, they remain local and removable by the user.

Access tokens, refresh tokens, ID tokens, browser cookies, client secrets, and
any value capable of proving identity or restoring access must never be stored
in the project. The OAuth client ID identifies the Meshingress application, not
an account, and cannot itself establish whether an account remains available in
the browser.

On opening a project, Studio preserves any active login. It may use the
provider's supported browser/session flow to non-destructively confirm a
remembered profile. A confirmed identity is offered under **Other profiles**;
an unavailable or expired identity remains benched. Selecting another profile
is an explicit switch, and logout remains explicit. A project record may only
suggest an identity: the provider and server independently verify it before
use, and Aegis continues to bind profiles by verified `issuer` + `subject`.
