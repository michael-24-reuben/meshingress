# Notes

## Rating From Planning

Estimated final design rating after refinement:

- Flexibility: 8.5 / 10
- Security: 8.7 / 10
- Robustness: 8.8 / 10
- Simplicity: 7.8 / 10
- Type safety: 8.5 / 10
- Debuggability: 8.5 / 10
- Controller readability: 9 / 10
- Overall: 8.7 / 10

## Key Tradeoff

The design improves controller readability by moving cross-cutting route behavior into annotations. That creates framework complexity behind the scenes. Startup validation, route introspection, structured logs, and metrics are required to keep the behavior inspectable.

## Recommended First Implementation Slice

Start with:

1. `HTTPRequest`
2. `HTTPResponse`
3. `@McpRoute`
4. `McpMiddleware` interface
5. `@McpRequestMiddleware`
6. `@McpConfigureMapping`
7. `@EnableWithinTimeRanges`
8. startup validation
9. structured route execution logs

Keep this slice inside the three route library modules only:

- `lib/meshingress-route-api`
- `lib/meshingress-route-annotations`
- `lib/meshingress-route-framework`

Do not create a centralized route aggregate module and do not attach the implementation to `app/meshingress-server` yet. Add more availability annotations, metrics, and live server routes after the library modules compile and have isolated tests.

## Module Naming Note

`meshingress-route-framework` is accepted as the runtime module name. It should contain the framework machinery, not just annotations. The preferred supporting modules are `meshingress-route-api` and `meshingress-route-annotations`.

## 2026-05-21 First Library Slice

Implemented the first code slice as three `lib/` modules only:

- `lib/meshingress-route-api`
- `lib/meshingress-route-annotations`
- `lib/meshingress-route-framework`

No centralized `lib/meshingress-route` aggregate module was introduced. No dependencies or route implementation wiring were added to `app/meshingress-server`.

Implemented source coverage:

- request/response/error/context contracts
- middleware and availability policy contracts
- route, middleware, secret, configuration, HTTP method, availability mode, and stability annotations
- specific availability annotations for time ranges, days, and feature flags
- framework scanner, registry, validator, middleware executor, availability evaluator, execution pipeline, standard error mapper, and structured logging observer

Verification:

```powershell
.\mvnw.cmd -pl lib/meshingress-route-framework -am test "-Djava.version=22" "-Dmaven.compiler.release=22" "-Dmaven.compiler.source=22" "-Dmaven.compiler.target=22"
```

Result: build success, 6 route-framework tests passed.
