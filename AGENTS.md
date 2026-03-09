---
# Local Setup Dasarathi

## Profile Load
- Use `zsh` for shell commands.
- Project root: `/Users/dasarathi/Development/WSELF/OCI-DevOps-ToolKit`.
- Local Specify CLI: `/Users/dasarathi/Application/BIN/specify`.

## Spec-Driven Development (SDD)

This repository uses Spec Kit style spec-driven development.

### Required Flow
- Before implementing a feature or behavior change, create or update a spec under `specs/<version-or-number>-<slug>/`.
- Baseline release specs use compact semantic version directory names such as `specs/v100` for version `1.0.0`.
- Keep the project constitution in `.specify/memory/constitution.md` authoritative.
- Use `.specify/templates/spec-template.md`, `.specify/templates/plan-template.md`, and `.specify/templates/tasks-template.md` for new specs.
- Do not treat code as the only source of truth. Align docs, specs, tests, and implementation.
- For fixes that are too small for a full feature spec, still record the intent in the nearest active spec or create a focused maintenance spec.

### Current Baseline Spec
- Current project baseline: `specs/v100/spec.md`
- Current technical plan: `specs/v100/plan.md`
- Current task backlog: `specs/v100/tasks.md`

### Quality Gates
- Java code must follow existing package structure and IntelliJ Platform conventions.
- Remote OCI SDK calls and CLI commands must run off the Swing UI thread.
- Swing UI mutations must return to the IntelliJ application/UI thread.
- OCI tenancy, compartment, project, endpoint, profile, and AI backend values must not be expanded further as hard-coded constants unless the active spec explicitly accepts that tradeoff.
- New behavior should include focused tests where it can be verified without live OCI access.
- Run the narrowest useful Gradle verification before handoff; if verification cannot complete, document the exact reason.
---
