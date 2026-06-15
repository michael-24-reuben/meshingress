# Meshingress CodeQL Scope Sample

This module is an isolated experiment for the artifact scope review design.

It shows one policy shape:

```txt
predefined Meshingress scope catalog
        -> generated CodeQL query text for source/build-aware analysis
        -> bytecode scan for JAR-only uploads
        -> normalized scope findings
```

The local CodeQL checkout lives under `vendor/codeql`. It is intentionally not used as a Java library; CodeQL is a query/runtime tool. The Java sample generates query text from the same scope rules and uses ASM for the JAR-only sample path.

Sample JAR target:

```txt
../../temp/sample-module-0.0.1-SNAPSHOT-all.jar
```

Run only this sample:

```powershell
.\mvnw.cmd -pl lib/meshingress-codeql-scope-sample test
```
