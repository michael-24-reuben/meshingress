# Verification

## Commands

```powershell
.\mvnw.cmd -f packages\aegis\pom.xml test
.\mvnw.cmd -pl app/meshingress-server -am '-Dtest=ToolPolicyServiceTest,SecurityDispatchContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

## Result

- Aegis: 19 tests passed; no failures, errors, or skips.
- Focused server reactor: 3 tests passed; no failures, errors, or skips.
- The policy test proves tenant isolation, priority selection, deny-overrides,
  and required-grant denial using a durable H2 JDBC store.
- The contract test proves the approved dispatch names are registered and that
  `roles/security/credential-bindings/verify` is absent.

## Known verification boundary

No real identity-provider issuer is configured in this local development
workspace. The next deployment step must provide an issuer URI and exact
audience, then exercise a real signed JWT over both HTTP and WebSocket.
