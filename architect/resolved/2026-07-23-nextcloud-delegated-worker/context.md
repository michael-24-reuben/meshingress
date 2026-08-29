# Context

The intended single routing model is Nextcloud-owned media retrieval:

```text
RESERVE operation -> OPEN -> native-file uploads and URL delegation -> SEALED -> QUEUED -> RUNNING -> COMPLETED | FAILED
```

The final `seal` is the immutable handoff boundary. While `OPEN`, Meshingress may add generated native files and delegated HTTPS source jobs. After sealing, Nextcloud owns execution and Meshingress polls durable status.

The former `meshingress` app is retained as a local v1 reference. It must not be modified, deployed, deleted, or treated as the new plain-named app.
