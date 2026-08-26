# Assessment

The prior implementation was a token-addressed single-object lease. That model could not express a tool result containing multiple downloaded files, did not make the physical session/request layout visible in the contract, and gave tools no stable workspace abstraction. The accepted model is a short-lived workspace per server-generated storage request, grouped under the MCP session and addressed by named relative files.
