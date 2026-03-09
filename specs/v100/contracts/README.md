# Contracts: OCI DevOps ToolKit Baseline

Current contracts are defined by:

- IntelliJ extension points in `src/main/resources/META-INF/plugin.xml`.
- OCI Java SDK request/response models for DevOps resources.
- OCI CLI output for version, session authenticate, and session refresh commands.
- Gatling simulation parameters passed with `-D...` system properties.

No hand-written HTTP contract files are currently maintained.
