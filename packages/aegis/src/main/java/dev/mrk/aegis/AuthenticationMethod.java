package dev.mrk.aegis;

/** How an identity was authenticated by the host application. */
public enum AuthenticationMethod {
    OIDC,
    SERVICE_ACCOUNT,
    LOCAL_DEVELOPMENT
}
