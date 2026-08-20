package dev.mrk.meshingress.api.tools;

import dev.mrk.meshingress.api.McpDispatchHandler;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;

public interface McpToolHandler extends McpDispatchHandler<DispatchExecutionResult> {
    McpToolDescriptor descriptor();

    /** Type whose manifest owns this handler. Annotated adapters override this with the adapted tool class. */
    default Class<?> sourceType() {
        return getClass();
    }
}
