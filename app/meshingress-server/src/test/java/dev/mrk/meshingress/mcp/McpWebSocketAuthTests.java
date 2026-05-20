package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.auth.AuthStoreRegistry;
import dev.mrk.meshingress.auth.BootstrapAdminState;
import dev.mrk.meshingress.auth.FirstAdminRegistrationCommand;
import dev.mrk.meshingress.auth.FirstAdminRegistrationResult;
import dev.mrk.meshingress.auth.InvalidMcpCredentialException;
import dev.mrk.meshingress.auth.McpAuthenticatedSession;
import dev.mrk.meshingress.auth.McpCredentialValidator;
import dev.mrk.meshingress.controller.McpDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpWebSocketAuthTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void credentialValidatorAcceptsConfiguredOpaqueHeaders() {
        McpCredentialValidator validator = validator();

        McpAuthenticatedSession session = validator.validate(
                "Bearer dev-access-token",
                "dev-secret-key",
                "dev-auth-token"
        );

        assertThat(session.clientId()).isEqualTo("dev-client");
        assertThat(session.subject()).isEqualTo("dev-subject");
    }

    @Test
    void credentialValidatorRejectsMissingAndInvalidHeaders() {
        McpCredentialValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(null, "dev-secret-key", "dev-auth-token"))
                .isInstanceOf(InvalidMcpCredentialException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> validator.validate("Bearer wrong", "dev-secret-key", "dev-auth-token"))
                .isInstanceOf(InvalidMcpCredentialException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handshakeStoresAuthenticatedMcpContextAttributes() {
        McpAuthHandshakeInterceptor interceptor = new McpAuthHandshakeInterceptor(validator());
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();
        HttpHeaders headers = validHeaders();
        headers.set("X-Mcp-Admin", "true");
        headers.set("Mcp-Session-Id", "session-1");
        headers.set("X-Request-Id", "request-1");
        when(request.getHeaders()).thenReturn(headers);

        boolean accepted = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes.get(McpAuthHandshakeInterceptor.AUTHENTICATED_SESSION_ATTRIBUTE))
                .isInstanceOf(McpAuthenticatedSession.class);
        assertThat(attributes.get(McpAuthHandshakeInterceptor.AUTHORIZATION_ATTRIBUTE))
                .isEqualTo("Bearer dev-access-token");
        assertThat(attributes.get(McpAuthHandshakeInterceptor.ROLE_ATTRIBUTE)).isEqualTo("true");
        assertThat(attributes.get(McpAuthHandshakeInterceptor.SESSION_ID_ATTRIBUTE)).isEqualTo("session-1");
        assertThat(attributes.get(McpAuthHandshakeInterceptor.REQUEST_ID_ATTRIBUTE)).isEqualTo("request-1");
    }

    @Test
    void handshakeRejectsMissingAndInvalidCredentialsBeforeSessionIsEstablished() {
        McpAuthHandshakeInterceptor interceptor = new McpAuthHandshakeInterceptor(validator());
        ServerHttpRequest missingRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse missingResponse = mock(ServerHttpResponse.class);
        when(missingRequest.getHeaders()).thenReturn(new HttpHeaders());

        boolean missingAccepted = interceptor.beforeHandshake(
                missingRequest,
                missingResponse,
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertThat(missingAccepted).isFalse();
        verify(missingResponse).setStatusCode(HttpStatus.UNAUTHORIZED);

        ServerHttpRequest invalidRequest = mock(ServerHttpRequest.class);
        ServerHttpResponse invalidResponse = mock(ServerHttpResponse.class);
        HttpHeaders invalidHeaders = validHeaders();
        invalidHeaders.set("X-Secret-Key", "wrong");
        when(invalidRequest.getHeaders()).thenReturn(invalidHeaders);

        boolean invalidAccepted = interceptor.beforeHandshake(
                invalidRequest,
                invalidResponse,
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertThat(invalidAccepted).isFalse();
        verify(invalidResponse).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void webSocketHandlerDelegatesTextFramesToExistingDispatcherWithMcpContext() throws Exception {
        McpDispatcher dispatcher = mock(McpDispatcher.class);
        JsonRpcResponses responses = new JsonRpcResponses(objectMapper);
        McpWebSocketHandler handler = new McpWebSocketHandler(objectMapper, dispatcher, responses);
        JsonNode dispatcherResponse = objectMapper.readTree("""
                {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
                """);
        when(dispatcher.dispatch(any(JsonNode.class), any(McpCallContext.class)))
                .thenReturn(Optional.of(dispatcherResponse));
        WebSocketSession session = authenticatedSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"jsonrpc":"2.0","id":1,"method":"ping"}
                """));

        ArgumentCaptor<McpCallContext> contextCaptor = ArgumentCaptor.forClass(McpCallContext.class);
        verify(dispatcher).dispatch(any(JsonNode.class), contextCaptor.capture());
        McpCallContext context = contextCaptor.getValue();
        assertThat(context.authorizationHeader()).isEqualTo("Bearer dev-access-token");
        assertThat(context.roleHeader()).isEqualTo("admin");
        assertThat(context.sessionId()).isEqualTo("session-1");
        assertThat(context.requestId()).isEqualTo("request-1");
        verify(session).sendMessage(new TextMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"ok\":true}}"));
    }

    @Test
    void webSocketHandlerSendsNoReplyForDispatcherNotificationEmptyResponse() throws Exception {
        McpDispatcher dispatcher = mock(McpDispatcher.class);
        McpWebSocketHandler handler = new McpWebSocketHandler(
                objectMapper,
                dispatcher,
                new JsonRpcResponses(objectMapper)
        );
        when(dispatcher.dispatch(any(JsonNode.class), any(McpCallContext.class))).thenReturn(Optional.empty());
        WebSocketSession session = authenticatedSession();

        handler.handleTextMessage(session, new TextMessage("""
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                """));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void webSocketHandlerReturnsParseErrorForMalformedJson() throws Exception {
        McpDispatcher dispatcher = mock(McpDispatcher.class);
        McpWebSocketHandler handler = new McpWebSocketHandler(
                objectMapper,
                dispatcher,
                new JsonRpcResponses(objectMapper)
        );
        WebSocketSession session = authenticatedSession();

        handler.handleTextMessage(session, new TextMessage("{"));

        verify(dispatcher, never()).dispatch(any(JsonNode.class), any(McpCallContext.class));
        verify(session).sendMessage(new TextMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error\"}}"
        ));
    }

    private McpCredentialValidator validator() {
        return new McpCredentialValidator(new FakeAuthStoreRegistry());
    }

    private HttpHeaders validHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer dev-access-token");
        headers.set("X-Secret-Key", "dev-secret-key");
        headers.set("X-Auth-Token", "dev-auth-token");
        return headers;
    }

    private WebSocketSession authenticatedSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
                McpAuthHandshakeInterceptor.AUTHENTICATED_SESSION_ATTRIBUTE,
                new McpAuthenticatedSession("dev-client", "dev-subject")
        );
        attributes.put(McpAuthHandshakeInterceptor.AUTHORIZATION_ATTRIBUTE, "Bearer dev-access-token");
        attributes.put(McpAuthHandshakeInterceptor.ROLE_ATTRIBUTE, "admin");
        attributes.put(McpAuthHandshakeInterceptor.SESSION_ID_ATTRIBUTE, "session-1");
        attributes.put(McpAuthHandshakeInterceptor.REQUEST_ID_ATTRIBUTE, "request-1");
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static class FakeAuthStoreRegistry implements AuthStoreRegistry {

        @Override
        public BootstrapAdminState bootstrapAdmin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAdmin() {
            return true;
        }

        @Override
        public FirstAdminRegistrationResult registerFirstAdmin(FirstAdminRegistrationCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<McpAuthenticatedSession> validateMcpCredentials(
                String authorization,
                String secretKey,
                String authToken
        ) {
            if ("Bearer dev-access-token".equals(authorization)
                    && "dev-secret-key".equals(secretKey)
                    && "dev-auth-token".equals(authToken)) {
                return Optional.of(new McpAuthenticatedSession("dev-client", "dev-subject"));
            }
            return Optional.empty();
        }

        @Override
        public boolean isAdminBearerToken(String authorization) {
            return "Bearer dev-access-token".equals(authorization);
        }
    }
}
