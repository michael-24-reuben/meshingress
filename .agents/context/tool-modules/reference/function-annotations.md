```java
package dev.mrk.meshingress.api.tools.annotation;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityCondition;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionAvailabilityCondition {
    Class<? extends McpAvailabilityCondition<? extends Annotation>> value();
}
```

[McpFunctionAvailabilityCondition.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpFunctionAvailabilityCondition.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionParam {
    String value();
    String description() default "";
    boolean required() default true;
    Class<?> implementation() default Void.class;
}
```

[McpFunctionParam.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpFunctionParam.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import org.intellij.lang.annotations.Pattern;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunction {
    @Pattern("[a-z][a-z0-9-]*")
    String value() default "";
    String title() default "";
    String description() default "";
    ToolVisibility visibility() default ToolVisibility.PUBLIC;
    boolean enabled() default true;
}
```

[McpFunction.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpFunction.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;
import dev.mrk.meshingress.api.tools.annotation.availability.McpAvailabilityPolicy;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpFunctionAvailabilityPolicy {
    Class<? extends McpAvailabilityPolicy<? extends Annotation>> value();
}
```

[McpFunctionAvailabilityPolicy.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpFunctionAvailabilityPolicy.java)
