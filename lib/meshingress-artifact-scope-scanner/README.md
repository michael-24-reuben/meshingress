# Meshingress Artifact Scope Scanner

Reusable bytecode scope inference module for repository artifact review.

This module does not read the rule database during every scan. The intended runtime shape is:

```txt
JSON scope rule catalog
        -> ScopeInferenceCatalog bean loaded once at startup
        -> BytecodeScopeScanner.scan(jar, catalog)
        -> ScopeFinding list
```

The first implementation uses ASM for direct JAR bytecode inspection. SootUp can be added later behind the same catalog model when richer bytecode call graph analysis is needed.

Run the module tests:

```powershell
.\mvnw.cmd -pl lib/meshingress-artifact-scope-scanner test
```
