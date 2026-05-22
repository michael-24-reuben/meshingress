# Assessment
## Diagnosis
`McpRouteValidator` was coupling startup validation to concrete built-in availability annotations through direct `instanceof` checks. That meant adding a new availability annotation required editing the central validator.
## Impacted Behavior
- Availability metadata validation was not extensible.
- The validator owned rules that belonged with the annotation-specific condition/validator types.
- New annotation types would have required repeated changes in `McpRouteValidator`.
## Final Assessment
The correct shape is annotation-declared validation: each availability annotation declares its Spring-managed condition class, and the validator resolves and invokes that class during route validation.
