# Studio Runtime Trace Drawer

Add a session-local `Runtime` tab to the Workflow Studio drawer. It should present the active or most recent live workflow execution as a console-style trace: controls, optional filters, a millisecond timeline, a variable/event table, and compact totals.

Timeline lanes represent concurrent execution levels. They must not allocate a permanent row to every tool.
