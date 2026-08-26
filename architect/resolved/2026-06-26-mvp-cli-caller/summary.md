# Summary

Implemented the MVP local CLI caller in `cli/nextcloud-mcp-cli`. The CLI can validate config, list MVP non-admin tools, test an account with a current-user probe, and invoke registered tools with JSON output. Tool calls use configured account scopes and resolve the real Nextcloud user id before invocation so WebDAV paths do not confuse internal account ids with login/user ids. Focused CLI verification and the full Maven reactor passed.
