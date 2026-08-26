# Context

The artifact repository already stores artifacts by group, artifact ID, and version. Existing tool classes have no supported injected referral to their artifact workspace; metadata loading may infer a JAR-adjacent directory, but that is not a tool storage API and dynamic installation loads JARs from the runtime cache.

The intended workspace identity is the full tool artifact coordinate, including version. For example:

```text
repository/artifacts/dev/mrk/toolspace/youtube/2.1.0/workspace/
```

The owner wants version isolation by default, while allowing a newer tool version to retrieve one compatible resource from an earlier version only when its code declares that delegation for the specific read. There must be no global or implicit inheritance.

The YouTube tool is a likely consumer for future migration-aware state, but this record is reusable for every attachable tool module. Credentials, refresh tokens, mutable databases, and executable content must not silently fall back to a prior version; they require a distinct, explicit migration or credential-lifecycle design.
