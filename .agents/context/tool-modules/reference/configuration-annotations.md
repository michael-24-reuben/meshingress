
```java
package dev.mrk.meshingress.api.tools.annotation;

public enum McpAvailabilityMode {ALL, ANY}
```

[McpAvailabilityMode.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpAvailabilityMode.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpConfigureMapping {
    McpSecret[] secrets() default {};
    McpAvailabilityMode availabilityMode() default McpAvailabilityMode.ALL;
    boolean audit() default false;
    boolean debugTrace() default false;
    long timeoutMs() default 0L;
}
```

[McpConfigureMapping.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpConfigureMapping.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;
import org.intellij.lang.annotations.Pattern;

@Retention(RetentionPolicy.RUNTIME)
public @interface McpSecret {
    @Pattern("[A-Za-z][A-Za-z0-9._-]*")
    String name();
    @Pattern("[A-Za-z0-9._:/-]+")
    String ref();
}
```

[McpSecret.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpSecret.java)
