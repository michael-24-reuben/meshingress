package dev.mrk.meshingress.schema.constraint;

/** Image metadata is a specialization of the file constraint contract. */
public final class McpImageSchemaConstraint extends McpFileSchemaConstraint {

    public McpImageSchemaConstraint(String contentEncoding, String contentMediaType) {
        super(contentEncoding, contentMediaType);
    }

    public McpImageSchemaConstraint(String contentEncoding, String contentMediaType, long maxBytes) {
        super(contentEncoding, contentMediaType, maxBytes);
    }
}
