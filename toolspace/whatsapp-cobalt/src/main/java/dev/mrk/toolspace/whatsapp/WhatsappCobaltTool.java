package dev.mrk.toolspace.whatsapp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.node.ObjectNode;

@McpTool(
        value = "whatsapp.cobalt",
        title = "WhatsApp Cobalt",
        description = "Pair a local WhatsApp Web session and send text messages through Cobalt without exposing chat readers.",
        defaultFunction = "status"
)
@McpToolMapping("tools")
@McpToolScopes({
        McpToolScope.NETWORK_OUTBOUND,
        McpToolScope.EXTERNAL_API_WRITE,
        McpToolScope.MESSAGE_PUBLISH
})
public class WhatsappCobaltTool {

    private final WhatsappCobaltSessionService sessions;

    public WhatsappCobaltTool(WhatsappCobaltSessionService sessions) {
        this.sessions = sessions;
    }

    @McpConfigureMapping(timeoutMs = 5_000, audit = true)
    @McpFunction(
            value = "status",
            title = "Status",
            description = "Return the local WhatsApp Cobalt session status without exposing chats or messages."
    )
    public DispatchExecutionResult status(McpCallContext context) {
        ObjectNode status = sessions.status();
        return ok(status, "WhatsApp Cobalt status.");
    }

    @McpConfigureMapping(timeoutMs = 45_000, audit = true)
    @McpFunction(
            value = "start_pairing",
            title = "Start Pairing",
            description = "Start a WhatsApp Web pairing flow and return either a pairing code or raw QR payload."
    )
    public DispatchExecutionResult startPairing(WhatsappStartPairingArgs arguments, McpCallContext context) {
        try {
            ObjectNode result = sessions.startPairing(arguments);
            return ok(result, "WhatsApp pairing started.");
        } catch (Exception exception) {
            return failure("WHATSAPP_PAIRING_FAILED", exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 60_000, audit = true)
    @McpFunction(
            value = "connect_registered",
            title = "Connect Registered Session",
            description = "Connect a previously paired WhatsApp Web session by alias."
    )
    public DispatchExecutionResult connectRegistered(WhatsappConnectArgs arguments, McpCallContext context) {
        try {
            ObjectNode result = sessions.connectRegistered(arguments);
            return ok(result, "WhatsApp registered session connected.");
        } catch (Exception exception) {
            return failure("WHATSAPP_CONNECT_FAILED", exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 45_000, audit = true)
    @McpFunction(
            value = "send_text",
            title = "Send Text",
            description = "Send a text message to a WhatsApp phone number or JID. Does not read chats or messages."
    )
    public DispatchExecutionResult sendText(WhatsappSendTextArgs arguments, McpCallContext context) {
        try {
            ObjectNode result = sessions.sendText(arguments);
            return ok(result, "WhatsApp text message sent.");
        } catch (Exception exception) {
            return failure("WHATSAPP_SEND_TEXT_FAILED", exception);
        }
    }

    @McpConfigureMapping(timeoutMs = 60_000, audit = true)
    @McpFunction(
            value = "disconnect",
            title = "Disconnect",
            description = "Disconnect the active WhatsApp session, or log out when requested."
    )
    public DispatchExecutionResult disconnect(WhatsappDisconnectArgs arguments, McpCallContext context) {
        try {
            ObjectNode result = sessions.disconnect(arguments);
            return ok(result, "WhatsApp session disconnected.");
        } catch (Exception exception) {
            return failure("WHATSAPP_DISCONNECT_FAILED", exception);
        }
    }

    private static DispatchExecutionResult ok(ObjectNode result, String summary) {
        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.json(result))
                .structuredContent(result)
                .status("ok")
                .summary(summary)
                .build();
    }

    private static DispatchExecutionResult failure(String code, Exception exception) {
        return DispatchExecutionResult.builder()
                .error(code, exception.getMessage())
                .status("failed")
                .summary(exception.getMessage())
                .build();
    }
}
