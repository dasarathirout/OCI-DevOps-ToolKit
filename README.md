# OCI DevOps ToolKit

IntelliJ Platform plugin for selected Oracle Cloud Infrastructure (OCI) DevOps workflows.

## Overview

The current implementation provides an IDE tool window with four functional areas:

- **Repository**: OCI DevOps project, repository, and pull request visibility
- **Build**: build run visibility and basic build run details
- **Deployment**: deployment, deploy environment, and deploy pipeline visibility
- **Footer**: OCI CLI version, session state, and session refresh/auth controls

The most complete workflow in the current codebase is the **Repository** experience.

## Implemented Features

### Repository

- Loads OCI DevOps project summaries
- Loads repositories for the resolved DevOps project
- Loads pull request summaries for the selected repository
- Supports pull request filtering by:
  - user
  - status
  - branch
  - sort order
- Supports text search in the pull request list
- Provides refresh actions and empty/loading states

### Footer / OCI CLI Integration

- Detects OCI CLI version using `oci --version`
- Tracks local OCI session expiration state
- Triggers:
  - `oci session authenticate`
  - `oci session refresh`
- Periodically refreshes footer session state
- Opens OCI CLI settings from the tool window footer

### Platform Integration

- IntelliJ tool window registration
- Post-startup guard activity
- IntelliJ notification group integration
- Background task execution for OCI SDK and CLI calls

## Current Scope Notes

- **Build** tab lists recent build runs and displays selected run details where OCI access allows it.
- **Deployment** tab lists deployments and deploy environments and displays selected deployment, environment, and pipeline details where OCI access allows it.
- Repository and footer remain the most mature workflows.

## Unimplemented Features

The following areas remain incomplete or intentionally narrow:

- **Builds**
  - Build Run execution flow
  - Build status, logs, and result visualization
- **Deployments**
  - Deployment pipeline execution flow
  - Deployment status and progress visualization

## Technical Specification

### Stack

- Java 21
- Gradle Kotlin DSL
- IntelliJ Platform plugin `org.jetbrains.intellij.platform` 2.16.0
- IntelliJ IDEA Community target: 2025.2
- OCI Java SDK v2
- SLF4J + Logback
- JUnit 5 + Mockito
- Gatling Java DSL for HTTP performance simulations

### Plugin Registration

Defined in `src/main/resources/META-INF/plugin.xml`:

- tool window
- notification group
- post-startup activity

### Main Packages

- `dasarathi.devops.toolkit.core` - tool window composition, constants, settings, utilities
- `dasarathi.devops.toolkit.gui.repositories` - repository UI and OCI DevOps data loading
- `dasarathi.devops.toolkit.gui.footer` - footer UI and OCI CLI/session handling
- `dasarathi.devops.toolkit.gui.builds` - build run UI and OCI DevOps data loading
- `dasarathi.devops.toolkit.gui.deployments` - deployment UI and OCI DevOps data loading
- `dasarathi.devops.toolkit.cli` - OCI CLI command execution wrappers
- `dasarathi.devops.toolkit.event` - notifications

## Runtime Behavior

- OCI SDK requests execute in IntelliJ background tasks.
- OCI CLI commands execute asynchronously and update the UI on the IDE application thread.
- Pull request data is cached in project user data during the active IDE session.
- Footer session state is refreshed on a timer.

## Prerequisites

- JDK 21
- IntelliJ IDEA 2025.2 compatible build
- OCI CLI installed and available on `PATH`
- Valid local OCI session/profile configuration

## Build

```bash
./gradlew clean build
```

## Run

```bash
./gradlew runIde
```

## Test

```bash
./gradlew test
```

## Performance Tests

Gatling simulations live under `src/gatling/java` and generate HTML reports under
`build/reports/gatling`.

Run the credential-free smoke simulation:

```bash
./gradlew gatlingRun-dasarathi.devops.toolkit.perf.DevOpsApiSmokeSimulation
```

Run with a custom target and load:

```bash
./gradlew gatlingRun-dasarathi.devops.toolkit.perf.DevOpsApiSmokeSimulation \
  -DbaseUrl=https://api-ecomm.gatling.io \
  -DsmokePath=/session \
  -Dvu=5 \
  -Dp95Millis=1500
```

Run the OCI DevOps-like repository flow against a local mock or explicitly configured endpoint:

```bash
./gradlew gatlingRun-dasarathi.devops.toolkit.perf.DevOpsRepositorySimulation \
  -DbaseUrl=http://localhost:8080 \
  -DprojectsPath=/devops/projects \
  -DrepositoriesPath=/devops/repositories \
  -DpullRequestsPath=/devops/pull-requests \
  -DpullRequestDetailPath=/devops/pull-requests/demo-pull-request \
  -Dvu=10 \
  -Diterations=5 \
  -DmaxFailedPercent=1.0 \
  -Dp95Millis=2000
```

For protected targets, pass an authorization header at runtime:

```bash
./gradlew gatlingRun-dasarathi.devops.toolkit.perf.DevOpsRepositorySimulation \
  -DbaseUrl=https://example.internal \
  -DauthHeader="Bearer <token>"
```

Do not commit credentials, OCI tokens, tenancy OCIDs, or environment-specific URLs in Gatling
source or resources.

## Project Layout

```text
src/main/java       Plugin source
src/main/resources  Plugin metadata, icons, messages, logging
src/test/java       Unit tests
src/gatling/java    Gatling Java performance simulations
src/gatling/resources Gatling runtime configuration
docs/               Supporting docs and screenshots
```

## Reference Docs

- `docs/IdeaLabTechSpec.html`
- `docs/DevOpsToolkit-TechSpec-v1.pdf`
- OCI CLI docs: https://docs.oracle.com/en-us/iaas/tools/oci-cli/latest/oci_cli_docs/index.html
- OCI DevOps docs: https://docs.oracle.com/en-us/iaas/Content/devops/using/home.htm
- OCI API docs: https://docs.oracle.com/en-us/iaas/api/#/en/devops/20210630/

## Idea Lab Links

- [IntelliJ Plugin for DevOps resources like pull request review, build logs, deployment infos](https://community.oracle.com/customerconnect/discussion/946018/intellij-plugin-for-devops-resources-like-pull-request-review-build-logs-deployment-infos/)
- [Team48_IntelliJDvOpsToolKitPlugin_PreHackWeek](https://oracle.sharepoint.com/:f:/r/teams/OraHacksInternal/Shared%20Documents/OraHacks/OraHacks2026/Hacking%20Submissions/Team48_IntelliJDvOpsToolKitPlugin_PreHackWeek?csf=1&web=1&e=w5rDbW)
- [Team48_IntelliJDvOpsToolKitPlugin_FinalSubmission](https://oracle.sharepoint.com/:f:/r/teams/OraHacksInternal/Shared%20Documents/OraHacks/OraHacks2026/Hacking%20Submissions/Team48_IntelliJDvOpsToolKitPlugin_FinalSubmission?csf=1&web=1&e=P0XVC4)

#OraHacks2026
