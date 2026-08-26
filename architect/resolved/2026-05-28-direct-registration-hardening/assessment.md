# Assessment

## Diagnosis

Direct experimental local-JAR registration required `localJar.checksumSha256` when configured, but the direct experimental strategy did not verify a supplied checksum before activating the local JAR module. The bundle strategy already verified local-JAR checksums before copying a JAR into the local Maven repository, so the gap was limited to the experimental direct-activation path.

## Boundary

Direct registration remains a development and smoke-test path. Repository-backed publication installation remains the production provenance path because it carries reviewed artifact metadata, embedded assessment output, and publication records. This resolution does not promote direct experimental or staging registration into a production approval channel.

## Additional Finding

The checksum mismatch test initially used `helloworld.text`, which is already present on the classpath. That caused bundle-override denial to short-circuit the request before checksum verification. The negative coverage now uses a non-bundled tool id so the test reaches the checksum verifier.
