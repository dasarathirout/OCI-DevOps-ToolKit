# Implementation Plan: [FEATURE NAME]

**Spec**: `specs/[version-or-number-slug]/spec.md`
**Created**: [YYYY-MM-DD]
**Status**: Draft

## Technical Summary

[Summarize implementation strategy, affected packages, and primary risk.]

## Constitution Check

- [ ] IntelliJ-native UI and APIs are used.
- [ ] Slow operations run off the UI thread.
- [ ] UI mutations return to the application/UI thread.
- [ ] New cloud context is configurable or explicitly justified.
- [ ] Non-live behavior has focused tests.
- [ ] Error handling is graceful and visible.

## Affected Areas

- `src/main/java/...`: [changes]
- `src/test/java/...`: [tests]
- `src/main/resources/...`: [plugin metadata/messages/icons]
- `docs/` or `README.md`: [docs]

## Design Decisions

| Decision | Rationale | Alternatives Considered |
| --- | --- | --- |
| [Decision] | [Why] | [Other options] |

## Verification Plan

- Unit: [test classes or commands]
- Integration/manual: [live OCI/CLI/IDE checks]
- Regression: [existing behavior to re-check]

## Risks

- [Risk and mitigation]

## Rollback Plan

[How to revert or disable the change safely.]
