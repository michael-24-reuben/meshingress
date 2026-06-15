# Persistent Assignment State

You are running unattended in the Meshingress checkout.

## General Objective

Complete the current assignment described by `architect/PAS.md` and `architect/ASSIGNMENT.md`. When the active assignment is genuinely complete and verified, resolve the active architect entry according to `architect/README.md`, then update both handoff files for the next upcoming task:

- `architect/PAS.md`
- `architect/ASSIGNMENT.md`

## Required Startup Checks

1. Read `AGENTS.md`, `architect/README.md`, `architect/PAS.md`, and `architect/ASSIGNMENT.md`.
2. Run `git branch --show-current` and verify it matches the expected branch in PAS.
3. Run `git status --short`.
4. Do not work on `main`.
5. Do not revert, move, delete, or clean up unrelated dirty worktree files.

## Current Sequencing Policy

Follow the live PAS first. As of this instruction, the intended sequence is:

1. Complete the embedded CycloneDX SBOM generator slice in `lib/meshingress-artifact-scope-scanner/`.
2. Wire `cyclonedx-sbom` into repository assessment storage in `app/meshingress-repository/`.
3. After the SBOM slice is verified, continue with security measures such as direct registration hardening.
4. Keep external scanner CLI integrations deferred until the embedded Java/library path is complete.

Treat uploaded `requestedScopes` as claims, not trusted authority. Preserve the requested/inferred/approved/denied scope separation.

## Work Rules

- Keep the implementation narrow to the current PAS next action.
- Prefer the repo's existing Maven modules, JSON-RPC conventions, architect lifecycle, and test patterns.
- Use focused Maven verification before broad verification.
- If a task is incomplete, update PAS with the files changed, what remains, whether it compiles, tests run, blockers, risks, and the safest next action.
- If a task is complete, resolve the architect record with `assessment.md`, `fixes.md`, `verification.md`, and `summary.md`.
- Only create a new architect when the work is real and no focused entry already exists.
- Create a commit only when the repository is coherent and the dirty state is understood; never include unrelated changes.

## Required Closeout

At the end of every run, update both:

- `architect/PAS.md`
- `architect/ASSIGNMENT.md`

Each file must clearly state:

- current branch and expected branch;
- active objective and lifecycle status;
- work completed in this run;
- files changed by this run;
- unfinished files;
- files to touch next;
- tests run and their results;
- blockers and risks;
- exact next action for the next scheduled or manual run.

If the active objective is fully completed, resolve its architect entry first, then make PAS and ASSIGNMENT point to the next upcoming task.
