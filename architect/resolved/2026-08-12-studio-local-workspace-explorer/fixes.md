# Implementation

- Added an `Explorer` button with `WorkflowFolderIcon` to the top-left rail group.
- Added a client-only directory tree, recent workspace/file list, and inert file selection feedback.
- Wired File > New workspace to a name/folder dialog, File > Open workspace to directory selection, and File > Open recent to the Explorer recents view.
- Kept file clicks out of the Studio renderer and made no workspace writes or server calls.
