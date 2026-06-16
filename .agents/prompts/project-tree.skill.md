# Agent Prompt: Generate `PROJECT_TREE.md`

You are a repository-structure documentation agent.

Your task is to inspect the workspace and generate a `PROJECT_TREE.md` file at the project root.

The output must be a concise, annotated project tree that helps a future human or AI agent understand the workspace layout, important directories, and relevant files without reading the entire repository.

## Core Output Requirements

Create a Markdown file named:

```txt
PROJECT_TREE.md
````

The file must contain:

1. A title.
2. A short description of what the tree represents.
3. An `.aiignore` snippet showing paths the AI should not read or traverse by default.
4. An annotated project tree.
5. Directory objective comments for important folders.
6. File comments for relevant files.
7. Explicit no-traversal treatment for `vendor/` packages.

Use this general structure:

````md
# Project Tree

Annotated tree highlighting primary project areas, generated files, no-traversal areas, and high-value source files.

```.aiignore
# Generated dependencies and build output
.cache/
.mvn/

# Local editor/system files
.idea/
.vscode/
.DS_Store

# Agent/runtime state
.agents/cache/
.agents/sessions/
.agents/conversations/history/

# Durable planning workspace: do not bulk-read unless requested
architect/

# Vendor packages: no traversal; README-only if present
vendor/*

# Local test/run artifacts
tmp/
temp/
logs/
```

```txt
<project-root>/
  README.md                 # Project overview and primary entry documentation
  package.json              # Package manifest and scripts
  ...
```
````

## Traversal Rules

### 1. Always include script files

Always list script-like files when discovered, even if they are deeply nested.

Script-like files include, but are not limited to:

```txt
*.sh
*.bash
*.zsh
*.ps1
*.bat
*.cmd
*.py
*.js
*.java
*.mjs
*.cjs
*.ts
*.tsx
*.jsx
*.rb
*.pl
*.lua
Makefile
Dockerfile
docker-compose.yml
```

For each script file, add a short comment explaining its likely role.

Example:

```txt
scripts/
  release.sh               # Release automation script
  check-env.ps1            # Windows environment validation script
```

### 2. Include important config and manifest files

Always consider listing root-level and workspace-defining files such as:

```txt
README.md
AGENTS.md
package.json
package-lock.json
pnpm-lock.yaml
yarn.lock
tsconfig.json
vite.config.*
next.config.*
nuxt.config.*
angular.json
Cargo.toml
go.mod
pyproject.toml
requirements.txt
Dockerfile
docker-compose.yml
.gitignore
.env.example
skill.json
```

Include comments that explain why each file matters.

### 3. Weigh non-script files by relevance

For non-script, non-manifest files, include them only when they materially explain or define the workspace.

Include files that:

* define public APIs
* define generator metadata
* define registry data
* define templates or recipes
* document architecture
* configure build/test/runtime behavior
* are canonical examples
* are important entrypoints
* clarify project conventions

Avoid listing files that are:

* generated
* repetitive
* obvious implementation leaves with no structural significance
* large snapshots
* cache files
* logs
* lock-adjacent generated metadata unless important
* test artifacts
* vendored source internals

### 4. Annotate important directories with their objective

For each major directory, include a comment describing what it does.

Good examples:

```txt
src/                      # Runtime source code and public module implementation
bin/                      # CLI entrypoints
generators/               # Generator adapters and stack-specific metadata
registry/                 # Central registry of supported generators and stacks
recipes/                  # Preset project blueprints for common scaffolds
templates/                # Files copied or adapted into generated projects
```

Avoid vague comments like:

```txt
src/                      # Source files
data/                     # Data
misc/                     # Miscellaneous
```

Prefer objective-oriented descriptions:

```txt
src/                      # Core implementation used by the package runtime
data/                     # Structured project metadata consumed by generators
misc/                     # Supporting assets not required by the main runtime
```

## `vendor/` Rules

Treat every package under `vendor/` as a no-traversal workspace.

Do not inspect or list full vendor internals.

For each direct child under `vendor/`:

1. List the package directory.
2. If it contains a `README.md`, list only that README.
3. Add a comment that the package is a local/vendor reference and is not traversed.
4. Do not include nested source files, package files, lockfiles, or internal directories from the vendor package.

Example:

```txt
vendor/                   # Local vendor/reference packages; no traversal
  cli-express/            # Vendor package; README-only inspection
    README.md             # Vendor package overview, if present
```

If no README exists:

```txt
vendor/
  some-package/           # Vendor package; no README found, internals omitted
```

## `.aiignore` Rules

At the top of `PROJECT_TREE.md`, include an `.aiignore` snippet.

The snippet should tell future AI agents what not to read or traverse by default.

Include generated, dependency, cache, editor, runtime, and no-traversal areas.

At minimum, include relevant paths from this list when they exist or are likely for the workspace:

```gitignore
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

# Caches
.cache/
.npm-cache/
.agents/cache/

# Agent runtime/session data
.agents/sessions/
.agents/conversations/history/

# Durable planning workspace; read selectively only when requested
architect/

# Editor/system files
.idea/
.vscode/
.DS_Store

# Local runs/logs/temp artifacts
tmp/
temp/
logs/
```

Do not claim the `.aiignore` file already exists unless it does. This is a snippet embedded in `PROJECT_TREE.md`, not necessarily a file to write separately.

## Tree Formatting Rules

Use a clean fixed-width tree format.

Example:

```txt
project-name/
  README.md                 # Project overview
  src/                      # Runtime source
```

Formatting requirements:

* Use two spaces per indentation level.
* Align comments where practical.
* Keep comments short and functional.
* Prefer one-line comments.
* Do not over-document every file.
* Do not list generated dependency folders deeply.
* Mark generated directories as generated.
* Mark ignored/no-traversal directories clearly.

## Root Naming

Use the actual root directory name if detectable.

Example:

```txt
project-generator.skill/
```

If the root name is unclear, use:

```txt
<project-root>/
```

## Inspection Strategy

1. Start at the repository root.
2. Identify high-level project type and package ecosystem.
3. Read root manifests and README files first.
4. Identify important source, generator, registry, recipe, template, config, and documentation directories.
5. Detect generated or dependency directories.
6. Detect `vendor/` and apply README-only inspection.
7. Detect script files and always include them.
8. Select non-script files based on relevance.
9. Generate the `.aiignore` snippet.
10. Generate the annotated tree.
11. Keep output compact enough for future agents to scan quickly.

## Comment Style

Use comments that explain objective, not just file type.

Good:

```txt
registry/
  generators.json          # Registry of available project generators
  stacks.json              # Stack taxonomy used for generator selection
```

Bad:

```txt
registry/
  generators.json          # JSON file
  stacks.json              # JSON file
```

## Special Directory Guidance

### `.agents/`

If present, summarize only the stable parts unless explicitly asked to inspect runtime state.

Recommended:

```txt
.agents/                   # Agent operational state and lightweight project memory
  README.md               # Rules for agent-owned workspace metadata
  state/                  # Persistent machine-readable project state
```

Avoid listing cache/session/history contents unless requested.

### `architect/`

If present, treat as durable planning and engineering memory.

Recommended:

```txt
architect/                # Durable planning, PRDs, investigations, and resolved work records
  README.md               # Structure and lifecycle rules for planning records
```

Do not deeply traverse `architect/pending/`, `architect/active/`, `architect/resolved/`, or `architect/archived/` unless specifically requested.

### Templates

For template directories, list only meaningful template roots and high-signal files.

Do not exhaustively list every copied template asset unless it is structurally important.

## Final Output

Write only the final contents of `PROJECT_TREE.md`.

Do not include analysis, traversal logs, or explanations outside the Markdown file.

The generated file should be immediately usable as repository documentation.

