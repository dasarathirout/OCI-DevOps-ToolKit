# OCI DevOps ToolKit Constitution

## Purpose

OCI DevOps ToolKit is an IntelliJ Platform plugin that reduces developer context switching by bringing selected Oracle Cloud Infrastructure DevOps workflows into the IDE. The product prioritizes repository and pull request workflows first, then build, deployment, CLI session health, and performance test workflows as they become mature.

## Principles

### I. IDE-Native DevOps Experience

All user-facing workflows must feel native to IntelliJ IDEA. Tool window panels, notifications, actions, background tasks, icons, threading, settings, and persistence must use IntelliJ Platform APIs and established project patterns.

### II. Non-Blocking Operations

OCI SDK calls, OCI CLI commands, filesystem scans, and other potentially slow operations must not block the Swing UI thread. Background work must use IntelliJ background tasks or equivalent asynchronous execution, and UI updates must return to the application/UI thread.

### III. Configurable Cloud Context

Cloud-specific context must be explicit and reviewable. Existing hard-coded OCI profile, endpoint, compartment, and project constants are tolerated only as current baseline debt. New features must prefer settings, project discovery, or documented spec decisions over adding more hard-coded environment values.

### IV. Spec Before Implementation

New features and meaningful behavior changes must begin with a spec under `specs/<version-or-number>-<slug>/`. The spec must state user value, scope, acceptance scenarios, dependencies, and non-goals before implementation begins. Implementation plans and tasks must trace back to the spec.

Baseline/release specs must use compact semantic version directory names: `specs/v100` represents version `1.0.0`, `specs/v110` represents `1.1.0`, and `specs/v101` represents `1.0.1`. Dotted version directory names such as `specs/v1.0.0` should not be used.

### V. Testable Without Live OCI When Possible

Core transformations, command construction, filtering, formatting, caching, and error handling must be covered by unit tests that do not require live OCI access. Live OCI-dependent tests may remain disabled or manual, but the spec must call out that limitation.

### VI. Graceful Failure and Clear Status

The plugin must fail safely when OCI CLI, OCI SDK, local session tokens, network access, or permissions are unavailable. Failures must produce actionable UI status, notification, or log context without freezing the IDE.

### VII. Small, Traceable Changes

Changes should stay scoped to the active spec. Avoid unrelated refactors, formatting churn, or hidden behavior changes. Each task should be traceable to a requirement or risk in the spec.

## Architecture Constraints

- Java 21 is the baseline language level.
- Gradle Kotlin DSL is the build system.
- IntelliJ Platform plugin registration remains in `src/main/resources/META-INF/plugin.xml`.
- OCI DevOps access goes through the OCI Java SDK and session-token authentication unless a spec defines an alternate mechanism.
- CLI integration goes through the existing command execution abstraction unless a spec defines a replacement.
- Repository, build, deployment, footer, and performance-test features remain separated by package and source-set ownership.

## Delivery Gates

Before a feature is considered complete:

- The relevant spec is updated with actual scope and acceptance criteria.
- `plan.md` records the technical approach and risk decisions.
- `tasks.md` records completed and deferred work.
- Focused unit tests are added or updated for non-live behavior.
- Manual/live verification gaps are documented when OCI or local credentials are required.
- README or docs are updated when user-visible behavior changes.

## Governance

This constitution overrides informal preferences when implementation choices conflict. Amendments require updating this file with a new dated entry, and active specs must be reviewed for impact.

Version: 1.0.0
Ratified: 2026-05-27
Last Amended: 2026-05-27
