package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;
import dev.mrk.meshingress.api.tools.annotation.McpInputSchema;
import lombok.Getter;

@Getter
@McpInputSchema(description = "Greet a person by name")
public final class HelloWorldGreetArgs {

    @McpInputField(description = "The name of the person to greet", required = true)
    public String name;
}
