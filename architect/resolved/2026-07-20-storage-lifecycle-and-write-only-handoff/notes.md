# Implementation Notes

- The pre-existing local workspace implementation and its tests were untracked work in this checkout. They are in scope for this lifecycle refactor; unrelated MCP/audit/Aegis changes remain untouched.
- Initial external provider: WebDAV. It must use conditional `PUT` (`If-None-Match: *`) and never issue a foreign read, listing, deletion, or overwrite operation.
- `external-external` stays configuration-valid only for a provider that declares `provider-session` staging. No WebDAV target may satisfy that lifecycle.
