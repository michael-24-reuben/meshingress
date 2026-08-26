# Assessment

The route surface lacked durable state, tenant ownership, an enforceable policy
domain, and a production identity path. The existing development bearer adapter
was correctly unsuitable for managing credentials or policy.

The completed implementation uses a deployment-configured OIDC JWT decoder
with issuer and audience validation, maps normalized verified claims into
`McpPrincipal`, and carries the verified WebSocket handshake principal into
message dispatch. Management routes require that OIDC identity plus `admin` or
`security.manage`; the development bearer token is rejected.

The initial policy language remains intentionally small: exact function name,
tenant, priority, effect, and required grants. Highest-priority matching rules
use deny-overrides. It is not an expression engine or external PDP.
