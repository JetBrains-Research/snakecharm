# AGENTS.md

Guidance for coding agents working on SnakeCharm. Read the task-specific documents below before
acting; detailed procedures and historical evidence belong in those documents.

## Project overview

SnakeCharm is a Kotlin IntelliJ Platform plugin for the Snakemake workflow language, built on the
bundled Python plugin's PSI and parser APIs. Most extension points use `language="Python"`.
It supports two languages: Snakemake (`Snakefile`, `*.smk`, `*.rule(s)`) and SmkSL, embedded
inside Python strings. See [README.md](README.md) for user-facing features.

## Required reading by task

Read the relevant sections, following their prerequisites; unrelated port histories are optional.

| Before doing this | Read |
|---|---|
| Configure the environment, build, or launch a sandbox IDE | [DEVELOPER.md](DEVELOPER.md) |
| Change Gradle, wrapper packaging, or CI integration | [Build and packaging](DEVELOPER.md#build-and-packaging); for dependency upgrades, also the porting checklist below |
| Run, add, or debug tests; change fixtures or test infrastructure | [Testing](docs/testing.md), including setup before the first run |
| Change parsing, PSI, completion, highlighting, or framework detection | Relevant sections of [Architecture](docs/architecture.md) |
| Upgrade the platform, Java, Kotlin, or IntelliJ Platform Gradle Plugin; investigate compatibility | [Porting checklist](docs/porting/README.md) and the relevant source/target platform notes linked there |
| Change plugin versions or prepare a release | Versioning below and [Release checklist](DEVELOPER.md#release-checklist) |

## Essential working rules

- `gradle.properties` owns platform selection and compatibility: `platformType`,
  `platformVersion`, `pluginSinceBuild`, `pluginUntilBuild`, and `platformBundledPlugins`.
  Compiler and library versions live in `gradle/libs.versions.toml`.
- Launch **Gradle itself** on the JDK specified by `javaVersion` and `.java-version`; keep
  those pins synchronized. Verify `"$JAVA_HOME/bin/java" -version` after switching branches.
  JDK selection pitfalls and setup are in [DEVELOPER.md](DEVELOPER.md#environment-setup).
- After editing test data, use `./gradlew cleanTest test` (with a focused filter while iterating).
  `cleanTest` does not clear the sandbox VFS. Follow [Testing](docs/testing.md) for fixtures,
  cache invalidation, and confirming that the intended tests actually ran.
- Start feature discovery at `src/main/resources/META-INF/plugin.xml`.
  Read Snakemake language levels from `snakemake_api.yaml`, including `defaultVersion`;
  do not copy a current version number from documentation.
- When changing behavior covered by a detailed guide, update that guide. Keep this file short:
  reusable procedures belong in topic docs; platform-specific evidence belongs in port history.

## Plugin versioning

`pluginVersion` uses `YEAR.RELEASE.PLUGIN_RELEASE`. The first two components match the
**minimum supported PyCharm/IntelliJ Platform release line**. The third is the plugin's own release
number: start at **1**, then increment consecutively within that line.

For example, the first plugin release requiring platform 2026.3 is `2026.3.1`, followed by
`2026.3.2`, `2026.3.3`, etc. Do not start at `.0` or synchronize the third component with
PyCharm's maintenance-release number. Plugin `2026.3.2` does not imply a requirement for
PyCharm `2026.3.2`.

A plugin release normally supports several platform maintenance releases within its declared
compatibility range. A platform maintenance release does not automatically require a new plugin
release; a compatibility break may require one. Extending support to a newer platform line while
retaining the same minimum does not change the first two components.

`pluginSinceBuild` and `pluginUntilBuild` declare compatibility; `platformVersion` selects
the build/test target. Every new `pluginVersion` must have a matching section in
[CHANGELOG.md](CHANGELOG.md). Follow the release checklist to ensure all changes being shipped
appear in that section.
