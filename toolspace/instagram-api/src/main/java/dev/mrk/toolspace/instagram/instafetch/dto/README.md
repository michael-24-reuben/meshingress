# Instagram GraphQL Java DTOs

Generated from the uploaded TypeScript interfaces.

## Package

`dev.mrk.meshingress.instagram.dto`

Copy `src/main/java/dev/mrk/meshingress/instagram/dto` into your Spring Boot project.

## Notes

- Uses Lombok bean classes: `@Data` + `@NoArgsConstructor`.
- Uses Jackson `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` for Instagram's snake_case JSON.
- Uses explicit `@JsonProperty` for `__typename` and `__isXDTGraphMediaInterface`.
- Maps TypeScript `unknown` to `Object` and `unknown[]` to `List<Object>`.
- Uses boxed primitives (`Boolean`, `Integer`, `Long`, `Double`) so missing/null JSON fields do not fail deserialization.

## Example

```java
ObjectMapper mapper = new ObjectMapper();
InstaGraphQLResponseRoot root = mapper.readValue(json, InstaGraphQLResponseRoot.class);
XdtShortcodeMedia media = root.getData().getXdtShortcodeMedia();
```
