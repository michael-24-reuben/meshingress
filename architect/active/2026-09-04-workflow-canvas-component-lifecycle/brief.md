# Workflow Canvas Component Lifecycle

Design a shared abstract lifecycle for the current workflow-canvas components:
canvas, slot, node, and path. The base must establish what input is parsed through
the constructor and `init()`, how `start()` activates rendering and event
participation, and how `stop()` / terminal cleanup behave.

The immediate complexity is `start()`: it must not become an unbounded place
where data parsing, component construction, subscriptions, and presentation
state are entangled.

This entry records the design only. It does not authorize replacing the current
React/honeycomb implementation.
