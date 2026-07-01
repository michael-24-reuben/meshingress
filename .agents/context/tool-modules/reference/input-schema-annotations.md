
```java
package dev.mrk.meshingress.api.tools.annotation;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpInputField {

    String value() default "";

    String description() default "";

    boolean required() default true;
}
```

[McpInputField.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpInputField.java)

---

```java
package dev.mrk.meshingress.api.tools.annotation;

import dev.mrk.meshingress.schema.McpJsonSchemaProvider;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpInputSchema {

    String description() default "";

    Class<? extends McpJsonSchemaProvider> provider() default McpJsonSchemaProvider.class;
}
```
[McpInputSchema.java](../../../../lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation/McpInputSchema.java)
