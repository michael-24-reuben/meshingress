# Plan

1. Add `lib/meshingress-tool-annotations` to the Maven reactor.
2. Provide tool-author annotations for tool, function, function parameter, annotation hints, mapping, and schema metadata.
3. Add scanner/model classes that can inspect annotated classes and expose normalized metadata without registering or invoking tools.
4. Add focused tests using a helloworld-style annotated sample.
5. Verify the new module builds without changing server execution behavior.
