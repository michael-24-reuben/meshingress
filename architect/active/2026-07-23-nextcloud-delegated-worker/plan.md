# Plan

1. Preserve the former app by renaming its local source directory to `meshingress-v1` without altering its contents.
2. Define the request, response, status, source-job, retry, and manifest contracts around the single delegated-worker lifecycle.
3. Create a clean, plainly named `meshingress` app; do not copy v1 wholesale.
4. Implement ownership validation, managed-workspace creation, native-file storage, delegated URL persistence, per-source retry, worker execution, and status reporting.
5. Add lifecycle and recovery tests, then prove a one-chapter completed workspace and reader flow before any retirement/deployment decision.
