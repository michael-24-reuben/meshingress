# Context

The existing controller dispatcher annotation code lives under `app/meshingress-server/src/main/java/dev/mrk/meshingress/controller/dispatch` and is server/controller-local.

Tool modules should keep depending on shared tool-author contracts rather than server internals. This change adds a separate library for annotation-based tool metadata so a later change can decide how annotated tools attach to `ToolRegistry` and `ToolExecutor`.

The user explicitly asked not to attach this to execution logic yet.
