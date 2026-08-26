# Assessment

The original Aegis profile model was a validated immutable value but had no
ordinary profile lifecycle, host persistence boundary, conflict detection, or
safe way to mutate identity and credential-binding relationships. Bootstrap
enrollment intentionally owns only the first-administrator transaction and is
not a substitute for this ordinary profile API.

The resolved design introduces a compare-and-set `ProfileStore` SPI and exposes
the persisted revision in `ProfileSnapshot`. It keeps durable production
persistence host-owned while `InMemoryProfileStore` proves the contract in
tests. The public values remain credential-safe: bindings carry
`SecretReference`, never raw secret material.
