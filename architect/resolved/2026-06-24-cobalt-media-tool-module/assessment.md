# Assessment

`imputnet/cobalt` is a Node/Express API service, not a Java library that can be directly embedded into Meshingress. The correct integration shape is a Meshingress Java tool module that calls a configured Cobalt API instance.

The upstream source is still vendored inside the module at `toolspace/cobalt/upstream/cobalt` so a local instance can be run from inside the Meshingress checkout. The module does not default to the hosted public Cobalt API because upstream documentation says hosted instances are not intended for third-party project use without permission.
