# Assessment

The old `app/meshingress-tool-bundle` was already a one-way runtime classpath aggregator, but its location obscured its role and tool POMs repeated common infrastructure dependencies.

The final structure separates three Maven roles:

- `meshingress-tool-bom` is a `pom` import used only for shared version management.
- `meshingress-tool-starter` is a JAR that supplies the common tool API, annotations, manifest, and Spring auto-configuration classpath.
- `meshingress-tool-distribution` is an otherwise-empty JAR that lists shipped tool artifacts.

The distribution intentionally remains a JAR, not a `pom`: Spring Boot omitted transitive tool JARs when the server consumed a POM-packaged aggregator. Archive inspection established that the JAR aggregator preserves the required executable classpath behavior.

The follow-up usage sweep established a separate migration requirement: runtime defaults, tool-registration API values, checked-in OpenAPI snapshots, tests, and contributor guidance still used the bridge name. With those values left unchanged, new registration work would edit the compatibility POM instead of the distribution POM. The bridge POM and root reactor entry are the only legitimate uses of the old name.
