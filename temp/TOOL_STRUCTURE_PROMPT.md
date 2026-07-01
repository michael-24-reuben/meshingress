You are working in the Meshingress repository.

Create a README for developers that explains how to create a Meshingress tool module.

This README is not an architecture record and must not be written like an `architect/` entry. Do not use architect lifecycle language such as pending, active, resolved, assessment, blockers, or verification unless it is directly relevant to tool-module documentation.

## Goal

Write a practical developer-facing README that showcases how to build a Meshingress tool module from scratch.

The README should explain:

- what a Meshingress tool module is
- where tool modules live in the repo
- required Maven/module setup
- required dependencies
- required annotations
- optional annotations
- function declaration rules
- input parameter/schema rules
- result/response structure
- scopes/security requirements
- secrets/configuration requirements
- availability/enablement rules
- server registration/attachment requirements
- a minimal working example
- common mistakes
- a checklist for adding a new module

## Files and concepts to review first

Review the following repo areas before writing:

### 1. Maven/module structure

Inspect the root `pom.xml`.

Identify:

- parent project coordinates
- Java version
- packaging type
- module list
- where `lib/meshingress-tool-api` lives
- where `lib/meshingress-tool-annotations` lives
- where `toolspace/*` modules live
- examples such as `toolspace/helloworld` and `toolspace/instagram-api`

Then inspect the Meshingress server POM.

Identify:

- how the server depends on `meshingress-tool-api`
- how the server depends on `meshingress-tool-annotations`
- how the server attaches toolspace modules as Maven dependencies
- what a new tool module must add so the server can discover/load it

### 2. Tool API contracts

Review the tool API interfaces:

- `McpDispatchHandler`
- `McpToolHandler`
- any descriptor-related classes available in the API package

Document:

- what method a tool handler must implement
- what arguments are passed into a call
- what context object is available
- what return type is expected
- whether a descriptor is required
- how this differs from annotation-based tool declaration, if applicable

### 3. Result/output model

Review:

- `DispatchExecutionResult`
- `ResultContent`

Document:

- how to return text content
- how to return JSON/object/array content
- how to return structured content
- how to mark errors
- how status, summary, errorCode, errorMessage, and `_meta` are represented
- recommended response patterns for successful calls and failed calls

Include a small Java example that returns:

- a text result
- a structured JSON result
- an error result

### 4. Tool annotations

Review all annotation classes under the Meshingress tool annotations package.

At minimum, document:

- `@McpTool`
- `@McpFunction`
- `@McpFunctionParam`
- `@McpInputField`
- `@McpInputSchema`
- `@McpToolMapping`
- `@McpConfigureMapping`
- `@McpSecret`
- `@McpToolScopes`
- `@McpToolAvailabilityCondition`
- `McpAvailabilityMode`

For each annotation, explain:

- target usage: class, method, parameter, field, record component, etc.
- required fields
- optional fields
- defaults
- naming rules
- when to use it
- when not to use it
- a small code example

### 5. Naming and invocation rules

From `@McpTool`, document the expected naming format for:

- tool ID
- invocation name

Explain:

- tool ID uniqueness
- version behavior
- default invocation behavior
- `defaultFunction`
- `handlerKey`
- `enabled`
- `visibility`
- `dynamic`

Include valid and invalid examples.

### 6. Function declaration rules

From `@McpFunction`, document:

- method-level function declaration
- function name
- title
- description
- visibility
- enabled flag

Explain how a tool can expose one or multiple functions.

Include a sample class with:

- one tool annotation
- two functions
- parameter annotations
- return values

### 7. Input modeling

Review:

- `@McpFunctionParam`
- `@McpInputField`
- `@McpInputSchema`

Document both styles if supported:

- direct annotated method parameters
- input DTO / record classes

Explain:

- required vs optional fields
- descriptions
- implementation type hints
- custom schema provider behavior
- how schema documentation should be written for agents

Include examples using a Java record as input.

### 8. Configuration, secrets, and availability

Review:

- `@McpConfigureMapping`
- `@McpSecret`
- `McpAvailabilityMode`
- `@McpToolAvailabilityCondition`

Document:

- how to declare required secrets
- expected secret name/ref format
- timeout configuration
- audit flag
- debugTrace flag
- availability mode `ALL` vs `ANY`
- custom availability conditions, if implemented in the repo

Warn developers not to hard-code secrets in tool code or README examples.

### 9. Scopes/security

Review:

- `McpToolScope`
- `@McpToolScopes`

Document:

- why scopes exist
- how to apply scopes at class level
- how to apply scopes at method/function level
- available scope values
- least-privilege guidance
- examples for local read, file write, network access, database access, shell execution, config access, and external API access

Include a security checklist.

### 10. Server/module attachment

Explain how a completed tool module becomes available to the Meshingress server.

Document:

- adding the module to the root Maven modules list
- adding the module dependency to the server POM
- expected groupId/artifactId/version pattern
- package naming conventions
- build command
- any server restart/reload requirement if visible from the repo

Do not invent runtime discovery behavior. If discovery is not obvious from the code, say that the README should mark it as “verify against current loader implementation.”

## README output requirements

The README should be named something like:

```txt
tool-module-README.md
````

or placed in the appropriate tool module documentation location if the repo already has one.

Use this structure:

```md
# Creating a Meshingress Tool Module

## Purpose

## Repository Layout

## Required Dependencies

## Minimal Module POM

## Registering the Module with the Server

## Minimal Tool Example

## Tool Annotation Reference

## Function Annotation Reference

## Input Schema and Parameters

## Returning Results

## Errors and Metadata

## Scopes and Permissions

## Secrets and Configuration

## Availability Conditions

## Testing a Tool Module

## Common Mistakes

## New Tool Module Checklist

## Full Example
```

## Style rules

* Write for a developer implementing a tool, not for an architect.
* Be concrete and example-heavy.
* Prefer short sections and code snippets.
* Do not speculate. If the repo does not prove something, label it as “verify in current implementation.”
* Do not describe unrelated project planning systems.
* Do not include architect directory instructions.
* Use Java and Maven examples.
* Keep examples minimal but complete enough to copy.
* Mention security implications for scopes and secrets.
* Include a final checklist developers can follow before opening a PR.

## Acceptance criteria

The README is acceptable only if it answers:

1. What files do I create for a new tool module?
2. What dependencies do I need?
3. What annotations do I use?
4. How do I expose functions?
5. How do I define inputs?
6. How do I return results?
7. How do I declare scopes?
8. How do I declare secrets/configuration?
9. How do I attach the module to the server?
10. What mistakes should I avoid?
11. What does a minimal working example look like?
