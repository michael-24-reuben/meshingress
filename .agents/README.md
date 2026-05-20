# `.agents/` Directory Guide

The `.agents/` directory is the local workspace used by project-building agents to store lightweight operational context, durable project state, conversation receipts, temporary cache data, and session progress.

This directory is **not** the application source code. It is support infrastructure for agents that plan, generate, modify, inspect, and coordinate the project.

---

## Purpose

The `.agents/` directory exists so agents can:

- remember what happened in previous conversations without storing full transcripts
- track the current project phase and active workflow
- store project decisions and approvals in machine-readable files
- cache expensive lookup results such as repository, dependency, or generator research
- resume work safely after interruption
- provide traceability for why files, plans, or scaffolding were created

The directory should stay small, structured, and readable by both humans and agents.

---

## Recommended Structure

```txt
.agents/
  README.md
  conversations/
    history/
      YYYY-MM-DD.jsonl
  sessions/
    active-session.json
  cache/
  state/
    project-state.json
    approvals.json
```

Optional future directories may be added as the agent system grows:

```txt
.agents/
  runs/
  checkpoints/
  generators/
  reports/
  indexes/
```

---

## Directory Responsibilities

### `.agents/`

Root directory for agent-owned metadata.

Files inside this directory are used to support the agent workflow. They should not be treated as normal product source files unless explicitly referenced by project tooling.

This root may contain:

```txt
.agents/README.md
```

The README explains the directory layout and rules for agents.

---

### `.agents/conversations/`

Stores records related to conversations between the user and agents.

This directory should contain durable summaries, not full transcripts.

Use this area for:

- conversation receipts
- summarized decisions
- high-level context from prior discussions
- links to related plans, issues, or generated artifacts

Do **not** store raw private reasoning, long transcripts, credentials, or unrelated chat logs here.

---

### `.agents/conversations/history/`

Stores append-only daily JSONL files.

Each file contains one JSON object per line. Each line represents a brief receipt of a conversation that took place on that day.

Use this naming format:

```txt
.agents/conversations/history/YYYY-MM-DD.jsonl
```

Example:

```txt
.agents/conversations/history/2026-05-15.jsonl
```

This is preferred over a single global history file because it keeps history easier to search, rotate, back up, review, and recover.

#### Rules

- Append one receipt per completed conversation or meaningful agent interaction.
- Do not rewrite old entries unless performing an explicit repair or migration.
- Do not store full transcripts.
- Keep each receipt short enough for agents to scan quickly.
- Use ISO-8601 timestamps.
- Prefer stable IDs that include the date and a short topic or agent name.

---

### `.agents/sessions/`

Stores temporary or active session state.

A session file represents what an agent is currently doing or what it last paused on.

Example:

```txt
.agents/sessions/active-session.json
```

Use this directory for:

- current active agent
- current task
- current phase
- last checkpoint
- pending user approval
- temporary workflow progress

Session data may change often.

Session files are useful for resuming work after a pause, crash, or user checkpoint.

---

### `.agents/cache/`

Stores rebuildable data.

Anything in this directory should be safe to delete.

Use this directory for:

- GitHub repository search results
- Maven/npm/package lookup results
- downloaded metadata
- generator discovery results
- dependency compatibility checks
- temporary analysis snapshots

Example files:

```txt
.agents/cache/github-search.spring-boot.json
.agents/cache/maven-artifacts.json
.agents/cache/generator-index.json
```

Cache files should not be treated as permanent truth. Agents may use them to avoid repeated work, but important decisions must be copied into `.agents/state/`, `.agents/conversations/history/`, or the `architect/` workspace.

---

### `.agents/state/`

Stores persistent machine-readable project state.

Unlike cache files, state files should not be casually deleted.

Use this directory for:

- selected project stack
- current project phase
- approved generators
- approved decisions
- known modules
- project configuration
- user approvals
- active blueprint metadata

Example files:

```txt
.agents/state/project-state.json
.agents/state/approvals.json
.agents/state/generator-state.json
```

State files should be compact and structured. They should represent the current truth of the project, not a historical log.

---

## Daily History File Format

History is stored as daily JSONL.

Each line is a complete JSON object.

Example file:

```txt
.agents/conversations/history/2026-05-15.jsonl
```

Example entry:

```json
{"schema":"agent.conversation.receipt.v1","id":"conv_20260515_044812_architecture","startedAt":"2026-05-15T04:48:12-04:00","endedAt":"2026-05-15T05:06:44-04:00","agent":"ArchitectureAgent","phase":"architecture","title":"Agent receipt file planning","summary":"Discussed where to store brief conversation receipts and decided to split history by day.","topics":["agent memory","jsonl","conversation history"],"inputs":["User wanted a renamed and better placed history file for agent conversation context."],"outputs":["Recommended .agents/conversations/history/YYYY-MM-DD.jsonl."],"decisions":["Use daily append-only JSONL files instead of one global file."],"next":["Create a writer utility that appends receipts after each meaningful agent interaction."],"confidence":0.92}
```

---

## Conversation Receipt Schema

```ts
export interface AgentConversationReceipt {
  /** Schema version for migration and compatibility. */
  schema: "agent.conversation.receipt.v1"

  /** Stable unique receipt ID. */
  id: string

  /** When the conversation or agent interaction started. */
  startedAt: string

  /** When the interaction ended, if known. */
  endedAt?: string

  /** Agent, role, or subsystem that handled the interaction. */
  agent: string

  /** Project phase at the time of the interaction. */
  phase?:
    | "idea"
    | "planning"
    | "architecture"
    | "building"
    | "testing"
    | "execution"
    | "deployment"
    | string

  /** Short human-readable label. */
  title: string

  /** Brief overview of what happened. This is not a transcript. */
  summary: string

  /** Searchable tags/topics. */
  topics?: string[]

  /** Important user-provided context. */
  inputs?: string[]

  /** Important generated results. */
  outputs?: string[]

  /** Decisions made during the interaction. */
  decisions?: string[]

  /** Follow-up work or unresolved items. */
  next?: string[]

  /** Optional references to related files, plans, PRDs, issues, commits, or artifacts. */
  refs?: string[]

  /** Agent-estimated confidence in the receipt accuracy. */
  confidence?: number
}
```

---

## Receipt Field Guidelines

### `schema`

Identifies the receipt format.

Always use:

```txt
agent.conversation.receipt.v1
```

Change this only when the schema changes in a breaking way.

---

### `id`

Unique ID for the receipt.

Recommended format:

```txt
conv_YYYYMMDD_HHMMSS_short-topic
```

Example:

```txt
conv_20260515_044812_architecture
```

---

### `startedAt` and `endedAt`

Use ISO-8601 timestamps with timezone offset.

Example:

```txt
2026-05-15T04:48:12-04:00
```

---

### `agent`

Name of the agent, role, or subsystem responsible for the conversation.

Examples:

```txt
ArchitectureAgent
PlanningAgent
ProjectStructureAgent
ResearchAgent
BuildAgent
TestingAgent
```

---

### `phase`

The project phase associated with the interaction.

Common values:

```txt
idea
planning
architecture
building
testing
execution
deployment
```

Agents may use custom phase names when needed, but should prefer the common phase names unless a more specific value is useful.

---

### `title`

Short label for the conversation receipt.

Good:

```txt
Agent receipt file planning
Spring Boot generator selection
Architecture checkpoint review
```

Bad:

```txt
Chat
Notes
Stuff we discussed
```

---

### `summary`

Brief explanation of what happened.

This should be a compressed overview, not a transcript.

Good:

```txt
Discussed how agents should store lightweight conversation receipts and decided to split history into daily JSONL files under .agents/conversations/history/.
```

Bad:

```txt
The user asked a question and the agent answered it.
```

---

### `topics`

Tags that help future agents search or filter the history.

Examples:

```json
["agent memory", "jsonl", "project scaffolding"]
```

---

### `inputs`

Important user-provided context.

This should summarize what the user asked for or clarified.

---

### `outputs`

Important generated results.

This may include recommended paths, schemas, decisions, files, or implementation plans.

---

### `decisions`

Finalized choices that future agents should respect.

Examples:

```json
["Use .agents/conversations/history/YYYY-MM-DD.jsonl for daily conversation receipts."]
```

---

### `next`

Follow-up tasks.

Examples:

```json
["Create a TypeScript utility that appends a receipt to today's history file."]
```

---

### `refs`

Optional references to related files or artifacts.

Examples:

```json
["architect/prd/project-blueprint-agent-system.md", "tree-view.json"]
```

---

### `confidence`

A number from `0` to `1` representing how confident the agent is that the receipt accurately captures the interaction.

Examples:

```json
0.72
0.9
1
```

---

## What Should Be Stored

Store:

- concise conversation summaries
- important decisions
- durable user preferences related to the project
- phase checkpoints
- generated artifact references
- unresolved next actions
- selected stack/generator choices
- links to plans, PRDs, issues, or project files

---

## What Should Not Be Stored

Do not store:

- full raw transcripts
- private chain-of-thought
- passwords
- API keys
- access tokens
- private keys
- unrelated personal information
- large generated files
- temporary logs that belong in cache
- raw tool output unless it is intentionally summarized

Sensitive values should be redacted or omitted.

---

## Relationship to `architect/`

The `.agents/` directory and `architect/` directory serve different purposes.

### `.agents/`

Agent runtime and coordination metadata.

Used for:

- current state
- receipts
- sessions
- cache
- resumability

### `architect/`

Human-readable planning and engineering workspace.

Used for:

- PRDs
- architecture plans
- implementation notes
- research reports
- issues
- drafts
- resolved records
- future work

If something is a durable project planning artifact, it usually belongs in `architect/`.

If something is agent operational context, it usually belongs in `.agents/`.

---

## Recommended Write Flow

When an agent completes a meaningful interaction:

1. Determine today's history file path.
2. Create the directory if it does not exist.
3. Build a compact conversation receipt.
4. Append the receipt as one JSON line.
5. Update `.agents/state/` only if the interaction changed current project truth.
6. Update `.agents/sessions/` only if the active workflow changed.
7. Store rebuildable lookup results in `.agents/cache/`.

Example path calculation:

```ts
const historyFile = `.agents/conversations/history/${yyyy}-${MM}-${dd}.jsonl`
```

---

## Recommended Read Flow

When an agent starts work:

1. Read `.agents/state/project-state.json` for current project truth.
2. Read `.agents/state/approvals.json` for accepted decisions.
3. Read `.agents/sessions/active-session.json` if resuming an active workflow.
4. Read recent files from `.agents/conversations/history/` for conversation context.
5. Use `.agents/cache/` only as a performance optimization.
6. Use `architect/` files for detailed planning context.

Agents should prefer the newest relevant state files over older history receipts when there is a conflict.

---

## Conflict Rules

If files disagree, prefer this order:

1. Explicit user instruction in the current conversation
2. `.agents/state/approvals.json`
3. `.agents/state/project-state.json`
4. Current `architect/` planning files
5. Recent `.agents/conversations/history/YYYY-MM-DD.jsonl` receipts
6. Older history receipts
7. `.agents/cache/`

Cache should never override state.

History should explain how decisions happened, but state should describe what is currently true.

---

## Versioning and Migration

All structured files should include a schema field when possible.

Examples:

```json
{"schema":"agent.project-state.v1"}
```

```json
{"schema":"agent.conversation.receipt.v1"}
```

When changing a file format in a breaking way:

1. Create a new schema version.
2. Keep old readers compatible when possible.
3. Write a migration script if needed.
4. Record the migration in the daily history file.

---

## Git Tracking Recommendation

Recommended to commit:

```txt
.agents/README.md
.agents/state/project-state.json
.agents/state/approvals.json
```

Recommended to ignore or selectively commit:

```txt
.agents/cache/
.agents/sessions/
.agents/conversations/history/
```

For solo/local workflows, history can be committed if it is useful.

For team or public repositories, review history carefully before committing because conversation receipts may contain project-sensitive context.

Suggested `.gitignore` rules:

```gitignore
.agents/cache/
.agents/sessions/
.agents/conversations/history/*.jsonl
```

If the project wants durable shared agent memory, remove the history ignore rule and enforce receipt sanitization.

---

## Minimal Initial Files

A minimal setup may start with only:

```txt
.agents/
  README.md
  conversations/
    history/
      2026-05-15.jsonl
  state/
    project-state.json
```

Everything else can be added when needed.

---

## Agent Rules Summary

Agents should follow these rules:

1. Keep `.agents/` small and structured.
2. Store conversation history as daily JSONL files.
3. Store receipts, not transcripts.
4. Keep cache rebuildable and disposable.
5. Keep state authoritative and compact.
6. Do not store secrets.
7. Do not store private reasoning.
8. Prefer ISO-8601 timestamps.
9. Append history instead of rewriting it.
10. Use `architect/` for durable planning artifacts.

---

## Final Recommended Layout

```txt
.agents/
  README.md
  conversations/
    history/
      YYYY-MM-DD.jsonl
  sessions/
    active-session.json
  cache/
  state/
    project-state.json
    approvals.json
```

This layout gives agents enough context to resume work, understand prior decisions, and coordinate future actions without turning `.agents/` into a bulky transcript archive.

