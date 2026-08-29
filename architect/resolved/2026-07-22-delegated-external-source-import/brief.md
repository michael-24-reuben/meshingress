# Brief

Implement `meshingress.storage.lifecycle=delegated-external` for tools that can provide source URLs instead of local bytes.

The lifecycle must collect source URL/output-path pairs in the tool workspace, submit one source-import blueprint to the installed Nextcloud Meshingress app at publication time, and expose the remote job status without having Meshingress download the delegated media.
