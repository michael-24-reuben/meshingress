# Summary

Storage lifecycle handoff is resolved for local serving and write-only WebDAV publication. Configuration is now provider-extensible without moving SQL metadata per target; all configured SQL table names are used. Foreign handoff is create-only and audit-only after publication, while local cleanup cannot contact foreign storage. A provider-session adapter remains the explicit prerequisite for `external-external`, so unsupported configurations fail at startup rather than weakening the lifecycle policy.
