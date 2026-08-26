# Todo

- [x] Inventory current repository endpoints and decide role requirements per endpoint.
- [x] Define repository roles for uploader, reviewer, publisher, and admin.
- [x] Add authorization enforcement to repository controllers or service boundaries.
- [x] Store append-only lifecycle events for upload, assess, approve, reject, publish, and revoke.
- [x] Define and implement revoke lifecycle behavior with append-only lifecycle events.
- [x] Define and implement delete lifecycle behavior with append-only lifecycle events.
- [x] Define and implement restore lifecycle behavior with append-only lifecycle events.
- [x] Add review queue/read APIs for pending artifacts and assessment details.
- [x] Add tests proving unauthorized users cannot approve or publish.
- [x] Add tests proving lifecycle events include actor, request id, timestamp, and state transition.
- [x] Decide whether first review surface is API-only or includes a minimal admin UI.
