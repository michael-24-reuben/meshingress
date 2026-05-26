package dev.mrk.meshingress.mcp.jsonrpc;

public final class JsonRpcErrorCodes {

    // Standard JSON-RPC 2.0 errors
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // Application/server-defined errors: -32000 to -32099
    public static final int SERVER_ERROR = -32000;
    public static final int UNAUTHORIZED = -32001;
    public static final int FORBIDDEN = -32002;
    public static final int RATE_LIMITED = -32003;
    public static final int TOOL_UNAVAILABLE = -32004;
    public static final int TOOL_TIMEOUT = -32005;
    public static final int TOOL_EXECUTION_FAILED = -32006;

    private JsonRpcErrorCodes() {}
}
