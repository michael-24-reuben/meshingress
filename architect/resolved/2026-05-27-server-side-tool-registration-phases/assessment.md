# Assessment

`roles/tools/register` now needs to serve two compatibility paths:

- Legacy descriptor registration, where clients pass a `tool` descriptor without a `phase`.
- Phase-aware server-side registration, where clients pass `phase` plus a phase-specific source object.

The implementation keeps the existing descriptor registration path intact, then routes phase-aware requests into a dedicated registration service. This prevents controller/service code from calling `ToolRuntimeLoader` directly and keeps dynamic install behavior behind phase-specific strategies.

The current server auth model exposes admin control-plane authorization, but it does not yet expose first-class bearer scope evaluation for `PLUGINS_INSTALL`, `TOOLS_REGISTER`, or `TOOLS_UPDATE`. This implementation therefore enforces the existing admin role gate and records the phase/source provenance. Native HTTP registration remains disabled by default through configuration.
