# Context

The first implementation correctly established the one-way Maven topology, but only the Maven dependency path moved to the new distribution module. The supplied search inventory shows several runtime and documentation defaults still naming `meshingress-tool-bundle`, which would direct new tool registration to the compatibility bridge instead of the canonical distribution.

The old `app/meshingress-tool-bundle` module remains deliberately present so existing Maven consumers can still resolve it. That is the only intended canonical-name exception, along with its root reactor declaration.
