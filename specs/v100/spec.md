# Feature Specification: OCI DevOps ToolKit Baseline

**Feature Branch**: `main`
**Created**: 2026-05-27
**Status**: Current

## User Value

Developers can review OCI DevOps repository, pull request, build, deployment, and local OCI CLI session context inside IntelliJ IDEA.

## Scope

### In Scope

- IntelliJ tool window for OCI DevOps workflows.
- Repository discovery from OCI DevOps Git remotes.
- Repository and pull request list/detail workflows.
- Pull request filters, search, comments, changed files, review actions, and merge action.
- OCI CLI version and session status footer.
- Build run list and detail visibility.
- Deployment, deploy environment, and deploy pipeline visibility.
- Gatling HTTP performance simulations and reports.
- Spec-driven project documentation.

### Out of Scope

- Replacing Swing or IntelliJ Platform APIs.
- Replacing OCI Java SDK integration.
- Implementing the full OCI DevOps Portal.
- Non-OCI DevOps troubleshooting workflows.
- Guaranteeing live OCI verification without local credentials and network access.

## Scenarios

### Scenario 1 - Repository Detection

**Given** an IntelliJ project has an OCI DevOps Git remote
**When** the project starts
**Then** namespace, project, and repository metadata are recorded for the tool window.

### Scenario 2 - Pull Request Review

**Given** OCI credentials are valid
**When** the Repository tab refreshes
**Then** the plugin loads pull requests, filters them, and displays selected details.

### Scenario 2a - Pull Request Diff Fallback

**Given** OCI DevOps pull request file-change APIs are unavailable
**When** the selected pull request has repository and branch context
**Then** the plugin attempts a local Git diff fallback before showing comment-only diff context.

### Scenario 2b - Pull Request File Patch Fallback

**Given** OCI DevOps file patch APIs are unavailable for a changed file
**When** the selected pull request has local Git branch context
**Then** the plugin attempts a local Git file patch fallback before showing summary-only file details.

### Scenario 3 - CLI Session Footer

**Given** OCI CLI is installed
**When** the footer refreshes
**Then** the plugin displays CLI version and session state and exposes authenticate/refresh actions.

### Scenario 3a - OCI SAML Session Authentication

**Given** the local OCI CLI authentication flow is started
**When** the plugin runs `oci session authenticate`
**Then** the command includes the configured OCNA SAML identity provider name.

### Scenario 4 - Build and Deployment Visibility

**Given** OCI access allows DevOps resource listing
**When** Build or Deployment tabs open
**Then** the plugin loads resource summaries and selected details without blocking the UI.

### Scenario 5 - Performance Reports

**Given** Gatling dependencies resolve
**When** a Gatling simulation runs
**Then** an HTML report is generated under `build/reports/gatling`.

## Requirements

- **FR-001**: Register an IntelliJ tool window for OCI DevOps workflows.
- **FR-002**: Start OCI CLI session authentication with identity provider `ocna-saml`.
- **FR-003**: Fall back to local Git diff metadata when OCI pull request file-change APIs fail.
- **FR-004**: Fall back to local Git file patch data when OCI repo file diff APIs fail.


## Dependencies

- Java 21.
- Gradle wrapper `9.3.1`.
- IntelliJ IDEA Community target `2025.2`.
- IntelliJ Platform Gradle plugin `2.16.0`.
- OCI Java SDK `2.82.0`.
- OCI CLI profile `OCI_DEVOPS_SCM`.
- Gatling Gradle plugin `3.15.1`.

## Data and State

- Project user data stores discovered repository context and loaded DevOps summaries.
- `PropertiesComponent` stores OCI CLI path, cached summaries, comments, and pull request review draft text.
- Current OCI endpoint, compartment OCID, project OCID, profile name, and OCI CLI identity provider name remain hard-coded.

## Current Risks

- Tool window registration id and lookup constant are not aligned.
- OCI cloud context is not fully configurable.
- Live OCI workflows require local credentials and network access.
- Gradle verification currently stalls after daemon startup in this environment.
