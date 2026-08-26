# Tool Module Catalog API

Expose each active tool module as a server-owned resource. A module document must provide bounded manifest data inline—metadata, license, property declarations, and requirements—and link to its README and icon.

The catalog must work for bundled/open-source modules loaded from the application classpath as well as runtime-installed artifact modules. It must not make `tools/list` carry full module documents, and it must not expose configured secret values or arbitrary module resources.
