# Workflow Canvas Pan State Race

Dragging the empty Workflow Studio canvas could leave the application blank.
The browser console identified an uncaught `TypeError` in `WorkflowCanvas` when
the pan transform updater read `originX` from a null gesture reference.

Resolve the focused client-side race without changing the unrelated root-route
and site-error-page planning entry.
