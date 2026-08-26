# Blocker

The implemented registration store is file-backed (`FileToolRegistrationStore`), not a database table. It persists records only after `ArtifactInstaller` activates a runtime module. The runtime loader registers its functions during activation, so a new artifact has no persisted contribution/precedence record when duplicate ownership is decided.

One owner decision is needed before source changes can preserve the intended semantics:

1. Retain the file-backed store and add a durable, normalized contribution-order document plus a reservation step before activation; or
2. Migrate registration state to SQL and create normalized registration/contribution-order tables before activation.

In both options, a reservation must exist before runtime activation. The current activate-then-save order cannot provide persisted, load-order-independent precedence for a first installation.
