package dev.mrk.meshingress.schema.constraint;

import tools.jackson.databind.node.ObjectNode;

/** Common schema metadata shared by file-like values. */
public class McpFileSchemaConstraint implements McpInputSchemaConstraint {

    private final String contentEncoding;
    private final String contentMediaType;
    private final long maxBytes;

    public McpFileSchemaConstraint(String contentEncoding, String contentMediaType) {
        this(contentEncoding, contentMediaType, -1);
    }

    public McpFileSchemaConstraint(String contentEncoding, String contentMediaType, long maxBytes) {
        this.contentEncoding = contentEncoding;
        this.contentMediaType = contentMediaType;
        this.maxBytes = maxBytes;
    }

    @Override
    public void applyTo(ObjectNode schema) {
        if (contentEncoding != null && !contentEncoding.isBlank()) {
            schema.put("contentEncoding", contentEncoding);
        }
        if (contentMediaType != null && !contentMediaType.isBlank()) {
            schema.put("contentMediaType", contentMediaType);
        }
        if (maxBytes >= 0) {
            schema.put("x-mcp-maxBytes", maxBytes);
        }
    }
}
