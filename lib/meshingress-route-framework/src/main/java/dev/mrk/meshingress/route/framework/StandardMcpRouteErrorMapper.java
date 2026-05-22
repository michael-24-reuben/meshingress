package dev.mrk.meshingress.route.framework;

import dev.mrk.meshingress.route.api.HTTPResponse;
import dev.mrk.meshingress.route.api.McpErrorResponse;
import dev.mrk.meshingress.route.api.McpRouteFrameworkException;
import dev.mrk.meshingress.route.api.McpRouteValidationException;

import java.util.Map;

public class StandardMcpRouteErrorMapper {

    public HTTPResponse<McpErrorResponse> map(Throwable throwable) {
        if (throwable instanceof McpRouteValidationException validationException) {
            return HTTPResponse.error(
                    500,
                    new McpErrorResponse(
                            validationException.code(),
                            validationException.getMessage(),
                            Map.of("violations", validationException.violations())
                    )
            );
        }
        if (throwable instanceof McpRouteFrameworkException frameworkException) {
            return HTTPResponse.error(
                    500,
                    McpErrorResponse.of(frameworkException.code(), frameworkException.getMessage())
            );
        }
        return HTTPResponse.error(
                500,
                McpErrorResponse.of("route.execution.failed", "Route execution failed")
        );
    }
}
