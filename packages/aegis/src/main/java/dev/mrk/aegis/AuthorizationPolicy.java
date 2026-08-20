package dev.mrk.aegis;

/** Host-provided authorization decision logic over a normalized request. */
@FunctionalInterface
public interface AuthorizationPolicy {
    AuthorizationDecision evaluate(AuthorizationRequest request);
}
