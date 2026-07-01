# Summary

Installed `imputnet/cobalt` as a new Meshingress tool module under `toolspace/cobalt`. The upstream source is vendored inside the module, and the Java MCP wrapper exposes `cobalt.info` plus `cobalt.process` against a configured Cobalt API instance. The module is attached to the Maven reactor and `meshingress-tool-bundle`, with focused unit and server MVC tests passing.
