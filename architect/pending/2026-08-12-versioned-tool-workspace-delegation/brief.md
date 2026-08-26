# Versioned Tool Workspace Delegation

Give each published tool coordinate its own workspace at:

```text
repository/artifacts/<group-path>/<artifact-id>/<version>/workspace/
```

A tool reads and writes only its own version workspace by default. A read may consult earlier version workspaces only when the tool code explicitly requests workspace-resource delegation. The lookup checks the current version first, then eligible prior versions in descending version order, and returns the first matching resource.

This is planning only. It does not add a workspace API, alter runtime loading, migrate files, or begin the YouTube API integration.
