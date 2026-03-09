# Implementation Plan: OCI DevOps ToolKit Baseline

**Spec**: `specs/v100/spec.md`
**Created**: 2026-05-27
**Status**: Current

## Technical Summary

The project is a Java 21 IntelliJ Platform plugin using Gradle Kotlin DSL, Swing UI, OCI Java SDK, OCI CLI commands, and Gatling performance simulations. Feature ownership is package-based: core/tool window, repository, footer, builds, deployments, CLI, and Gatling performance tests.

## Constitution Check

- [x] IntelliJ-native UI and APIs are used.
- [x] Slow SDK and CLI operations run off the UI thread.
- [x] UI mutations return to the application/UI thread.
- [ ] OCI cloud context is fully configurable.
- [x] Non-live behavior has focused tests.
- [x] Errors are reported through logs, status, or notifications.

## Affected Areas

- `src/main/java/dasarathi/devops/toolkit/core`: tool window, settings, constants, repository guard.
- `src/main/java/dasarathi/devops/toolkit/cli`: OCI CLI command execution.
- `src/main/java/dasarathi/devops/toolkit/gui/repositories`: repository and pull request UI.
- `src/main/java/dasarathi/devops/toolkit/gui/builds`: build run UI.
- `src/main/java/dasarathi/devops/toolkit/gui/deployments`: deployment UI.
- `src/main/java/dasarathi/devops/toolkit/gui/footer`: OCI CLI/session footer.
- `src/gatling`: HTTP performance simulations and Gatling resources.
- `specs`: current project specifications.

## Design Decisions

| Decision | Rationale |
| --- | --- |
| Use Swing and IntelliJ Platform APIs | Matches plugin runtime and existing UI ownership. |
| Use OCI Java SDK for DevOps data | Keeps typed service request/response models. |
| Use OCI CLI only for local CLI/session workflows | Keeps shell execution scoped. |
| Store short-lived summaries in project state and `PropertiesComponent` | Improves IDE responsiveness and restores useful state. |
| Keep Gatling outside `test` | Performance runs are environment-dependent and report-oriented. |

## Verification

- Unit: `./gradlew test`
- Formatting: `./gradlew spotlessCheck`
- Gatling compile: `./gradlew gatlingClasses`
- Gatling smoke: `./gradlew gatlingRun-dasarathi.devops.toolkit.perf.DevOpsApiSmokeSimulation`
- Manual IDE: `./gradlew runIde`
- Live OCI: verify Repository, PR, Build, Deployment, and Footer with valid `OCI_DEVOPS_SCM`.

## Open Work

- Align tool window id constant with `plugin.xml`.
- Move OCI endpoint, compartment, project, and profile values to settings or discovery.
- Restore reliable Gradle verification in this local environment.
