package dev.mrk.toolspace.instagram;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.McpToolDescriptor;
import dev.mrk.meshingress.api.tools.McpToolHandler;
import dev.mrk.meshingress.api.tools.ToolExecutionResult;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.toolspace.instagram.instafetch.FetchPath;
import dev.mrk.toolspace.instagram.instafetch.InstaFetch;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;


public class InstaFetchTool implements McpToolHandler {

    public InstaFetchTool() {
    }

    @Override
    public McpToolDescriptor descriptor() {
        JsonNode schema = JsonNodeFactory.instance.objectNode()
                .put("type", "object")
                .set("properties", JsonNodeFactory.instance.objectNode()
                        .set("url", JsonNodeFactory.instance.objectNode()
                                .put("type", "string")
                                .put("description", "The URL of the Instagram post to fetch data from.")
                        )
                )
                .set("required", JsonNodeFactory.instance.arrayNode().add("url"));
        ;
        ObjectNode annotations = JsonNodeFactory.instance.objectNode()
                .put("category", "social_media")
                .put("subcategory", "instagram")
                .put("icon", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Instagram_logo_2016.svg/1200px-Instagram_logo_2016.svg.png");
        return new McpToolDescriptor(
                "instagram.fetch",
                "Instagram Fetch",
                "Fetches data from Instagram based on a given URL.",
                1,
                true,
                ToolVisibility.PUBLIC,
                "instagram.fetch",
                schema,
                null,
                annotations,
                false

        );
    }

    @Override
    public ToolExecutionResult call(@NonNull ObjectNode arguments, McpCallContext context) {
        String url = arguments.get("url").asString();
        InstaFetch instafetch = new InstaFetch(FetchPath.asUrl(url));

        JsonNode response = instafetch.submitRequest();
        ArrayNode content = JsonNodeFactory.instance.arrayNode();
        content.add(response);

        return new ToolExecutionResult(content, response, false);
    }
}
