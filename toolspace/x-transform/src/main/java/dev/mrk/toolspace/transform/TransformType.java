package dev.mrk.toolspace.transform;

/** Stable Transform route identifiers, kept as enum values instead of per-transform MCP endpoints. */
public enum TransformType {
    SVG_TO_JSX("svg-to-jsx", "javascript"), SVG_TO_REACT_NATIVE("svg-to-react-native", "javascript"),
    HTML_TO_JSX("html-to-jsx", "javascript"), HTML_TO_PUG("html-to-pug", "pug"),
    JSON_TO_PROPTYPES("json-to-proptypes", "javascript"), JSON_TO_FLOW("json-to-flow", "flow"), JSON_TO_GRAPHQL("json-to-graphql", "graphql"), JSON_TO_TYPESCRIPT("json-to-typescript", "typescript"),
    JSON_TO_MOBX_STATE_TREE("json-to-mobx-state-tree", "javascript"), JSON_TO_SARCASTIC("json-to-sarcastic", "typescript"), JSON_TO_IO_TS("json-to-io-ts", "typescript"), JSON_TO_RUST_SERDE("json-to-rust-serde", "rust"),
    JSON_TO_MONGOOSE("json-to-mongoose", "javascript"), JSON_TO_BIG_QUERY("json-to-big-query", "javascript"), JSON_TO_MYSQL("json-to-mysql", "sql"), JSON_TO_SCALA_CASE_CLASS("json-to-scala-case-class", "scala"),
    JSON_TO_GO("json-to-go", "go"), JSON_TO_GO_BSON("json-to-go-bson", "go"), JSON_TO_YAML("json-to-yaml", "yaml"), JSON_TO_JSDOC("json-to-jsdoc", "javascript"), JSON_TO_KOTLIN("json-to-kotlin", "kotlin"),
    JSON_TO_JAVA("json-to-java", "java"), JSON_TO_JSON_SCHEMA("json-to-json-schema", "json"), JSON_TO_TOML("json-to-toml", "toml"), JSON_TO_ZOD("json-to-zod", "typescript"),
    JSON_SCHEMA_TO_TYPESCRIPT("json-schema-to-typescript", "typescript"), JSON_SCHEMA_TO_OPENAPI_SCHEMA("json-schema-to-openapi-schema", "json"), JSON_SCHEMA_TO_PROTOBUF("json-schema-to-protobuf", "protobuf"), JSON_SCHEMA_TO_ZOD("json-schema-to-zod", "typescript"),
    CSS_TO_JS("css-to-js", "javascript"), CSS_TO_TEMPLATE_LITERAL("object-styles-to-template-literal", "javascript"), CSS_TO_TAILWIND("css-to-tailwind", "css"),
    JS_OBJECT_TO_JSON("js-object-to-json", "json"), JS_OBJECT_TO_TYPESCRIPT("js-object-to-typescript", "typescript"),
    GRAPHQL_TO_TYPESCRIPT("graphql-to-typescript", "typescript"), GRAPHQL_TO_FLOW("graphql-to-flow", "flow"), GRAPHQL_TO_JAVA("graphql-to-java", "java"), GRAPHQL_TO_RESOLVERS_SIGNATURE("graphql-to-resolvers-signature", "typescript"),
    GRAPHQL_TO_INTROSPECTION_JSON("graphql-to-introspection-json", "json"), GRAPHQL_TO_SCHEMA_AST("graphql-to-schema-ast", "graphql"), GRAPHQL_TO_FRAGMENT_MATCHER("graphql-to-fragment-matcher", "typescript"),
    GRAPHQL_TO_COMPONENTS("graphql-to-components", "typescript"), GRAPHQL_TO_TYPESCRIPT_MONGODB("graphql-to-typescript-mongodb", "typescript"),
    JSONLD_TO_NQUADS("jsonld-to-nquads", "text"), JSONLD_TO_EXPANDED("jsonld-to-expanded", "json"), JSONLD_TO_COMPACTED("jsonld-to-compacted", "json"), JSONLD_TO_FLATTENED("jsonld-to-flattened", "json"),
    JSONLD_TO_FRAMED("jsonld-to-framed", "json"), JSONLD_TO_NORMALIZED("jsonld-to-normalized", "text"),
    TYPESCRIPT_TO_FLOW("typescript-to-flow", "flow"), TYPESCRIPT_TO_DECLARATION("typescript-to-typescript-declaration", "typescript"), TYPESCRIPT_TO_JSON_SCHEMA("typescript-to-json-schema", "json"), TYPESCRIPT_TO_JAVASCRIPT("typescript-to-javascript", "javascript"), TYPESCRIPT_TO_ZOD("typescript-to-zod", "typescript"),
    FLOW_TO_TYPESCRIPT("flow-to-typescript", "typescript"), FLOW_TO_DECLARATION("flow-to-typescript-declaration", "typescript"), FLOW_TO_JAVASCRIPT("flow-to-javascript", "javascript"),
    XML_TO_JSON("xml-to-json", "json"), YAML_TO_JSON("yaml-to-json", "json"), YAML_TO_TOML("yaml-to-toml", "toml"), TOML_TO_JSON("toml-to-json", "json"), TOML_TO_YAML("toml-to-yaml", "yaml"),
    MARKDOWN_TO_HTML("markdown-to-html", "html"), CADENCE_TO_GO("cadence-to-go", "go");

    private final String id;
    private final String outputLanguage;

    TransformType(String id, String outputLanguage) { this.id = id; this.outputLanguage = outputLanguage; }
    public String id() { return id; }
    public String outputLanguage() { return outputLanguage; }
}
