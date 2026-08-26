# Context

`AuthProfile` already validates versioned, credential-safe profile values. Its
identities own verified roles and scopes, and each credential binding references
an identity and can retain only `SecretReference`. The prior bootstrap record
creates exactly one first administrator but deliberately does not provide
ordinary profile storage or management.

This slice adds portable Aegis APIs and an in-memory reference store only. It
does not add Spring beans, persistence, secret-store calls, credential
verification, audit logging, HTTP endpoints, or MCP routes. A host must still
authorize every future management request and implement durable compare-and-set
persistence.

Removing a profile binding only removes Aegis metadata. It never revokes or
deletes the referenced external secret. Hosts must revoke upstream credentials
and destroy the secret independently before or after detaching the reference,
according to their incident and retention policy.
