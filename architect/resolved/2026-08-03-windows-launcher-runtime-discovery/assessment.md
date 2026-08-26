# Assessment

The Windows launcher was designed around its own `java -jar` process record. Its runtime matcher required a state-file PID and a command line containing `app/meshingress-server/target/meshingress.jar`.

The live service instead used Maven `spring-boot:run`, which launches the same application as `dev.mrk.meshingress.MeshingressApplication`. It was healthy and listening on the configured endpoint, but had no launcher state file and therefore appeared stopped.

The risk of broad Java process detection is avoided by requiring all of: the configured listener PID, a Java process, a command-line identity matching the packaged JAR or exact main class, and a successful actuator health probe.
