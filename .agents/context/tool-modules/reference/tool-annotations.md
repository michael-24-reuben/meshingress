```java
package dev.mrk.meshingress.api.tools.annotation;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import org.intellij.lang.annotations.Pattern;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
    /** Defines a unique tool ID, used to identify the tool in the system. 
     */
    @Pattern("[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]+)*")
    String value()/* default ""*/; // default is omitted because a tool needs to have an ID, and the empty string would be an invalid ID.
    String title() default "";
    String description() default "";
    int version() default 1;
    boolean enabled() default true;
    ToolVisibility visibility() default ToolVisibility.PUBLIC;
    String defaultFunction() default "main";
    String handlerKey() default "";
    boolean dynamic() default false;
}
```
[McpTool.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpTool.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolMapping {
    String value();
}
```
[McpToolMapping.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpToolMapping.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;
import dev.mrk.meshingress.scopes.McpToolScope;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolScopes {
    McpToolScope[] value() default {};
}
```

[McpToolScopes.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpToolScopes.java)
