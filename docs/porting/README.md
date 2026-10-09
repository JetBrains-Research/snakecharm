# Porting SnakeCharm across IntelliJ Platform releases

Use this checklist before changing the platform, Java, Kotlin, or IntelliJ Platform Gradle Plugin.
For environment setup and packaging, see [DEVELOPER.md](../../DEVELOPER.md); for test setup,
execution, and diagnosis, see [Testing](../testing.md).

## Port history

Read the relevant source and target platform notes before starting a port or removing a workaround.
Each records specific builds, fixes, rejected approaches, and historical validation results.

| Platform release line | Platform build | Engineering notes |
|---|---|---|
| 2026.1 | 261 | [Unified PyCharm and Python API migration](2026.1.md) — PR [#570](https://github.com/JetBrains-Research/snakecharm/pull/570) |
| 2026.2 | 262 | [Annotators, runtime libraries, fixtures, and sandbox wiring](2026.2.md) — PR [#577](https://github.com/JetBrains-Research/snakecharm/pull/577) |
| 2026.3 | 263 | [EAP API and completion changes](2026.3.md) — issue [#596](https://github.com/JetBrains-Research/snakecharm/issues/596) |

## Platform-update checklist

### Establish the target and baseline

1. Read `gradle.properties`, `gradle/libs.versions.toml`, `.java-version`, and the relevant
   port history. Capture the current test and verifier baseline.
2. Select the target using [build-number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
   and `./gradlew printProductsReleases`. That task lists only its configured channels;
   check released builds through [JetBrains' releases API](https://data.services.jetbrains.com/products/releases?code=PY&type=release&latest=false).
3. Before changing source, verify the **existing plugin binary** against the new IDE.
   Explicitly select the artifact and target; the repository's default `verifyPlugin` configuration
   may select a different pair. Preserve the binary-incompatibility report as a worklist:
   compilation alone does not expose every linkage problem.

### EAP and snapshot targets

Choose an unreleased target from the
[IntelliJ snapshots metadata](https://www.jetbrains.com/intellij-repository/snapshots/com/jetbrains/intellij/pycharm/pycharmPY/maven-metadata.xml).
A `-SNAPSHOT` target sets `useInstaller = false` in `build.gradle.kts`.
Pin a concrete build for comparable runs and record the downloaded IDE's `build.txt`;
release-line snapshots move. See the [2026.3 target](2026.3.md#build-infrastructure) for an example.

### Align toolchains and bundled libraries

Check these before debugging source or widespread test failures:

| Setting | Required check |
|---|---|
| `kotlin` in `gradle/libs.versions.toml` | Compiler can read the target's Kotlin metadata |
| `javaVersion` and `.java-version` | Both match the required bytecode/toolchain baseline; launch Gradle on that JDK too |
| `gradleVersion` and Gradle wrapper | Support that JDK and the build plugins |
| `intelliJPlatform` in the version catalog | Gradle plugin supports the target's modules, test runtime, and sandbox layout |
| `kotlinPlatform`, `kotlinxSerializationPlatform` | Match the libraries shipped by the target IDE |

Follow [JDK setup](../../DEVELOPER.md#environment-setup) and the
[IntelliJ Platform Gradle Plugin documentation](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).
Review related tooling such as Qodana when updating the build.

Read bundled library versions from the downloaded IDE rather than guessing. The known layout
supports these commands; adjust paths if the distribution changes:

```shell
unzip -p <ide>/lib/intellij.libraries.kotlinx.serialization.core.jar META-INF/MANIFEST.MF | grep Implementation-Version
# Kotlin is merged into util-8.jar; read the three KotlinVersion constructor integers.
javap -p -c -cp <ide>/lib/util-8.jar kotlin.KotlinVersionCurrentValue | sed -n '/KotlinVersion get/,/areturn/p'
```

Keep the runtime version overrides in `build.gradle.kts` aligned with these values.
For the failure mechanism and evidence, see [serialization ABI skew](2026.2.md#serialization-abi).

### Update compatibility and source

- Set `platformVersion`, `pluginSinceBuild`, and `pluginUntilBuild` deliberately.
  Advertise only verified compatibility; widening the manifest does not repair an incompatible binary.
- Follow [plugin versioning](../../AGENTS.md#plugin-versioning) and the
  [release checklist](../../DEVELOPER.md#release-checklist) when changing `pluginVersion`.
- Consult the target year's [API changes](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html).
  Check explicit module/plugin dependencies when a class or extension point disappears.
- Check replacement APIs' class-level status as well as method deprecation, and preserve the
  [PythonCore API boundary](../architecture.md#platform-api-boundaries).
- Review [wrapper packaging and CI configuration](../../DEVELOPER.md#build-and-packaging).

## Verification and acceptance

Compile production and test sources, build the distributable, and run focused tests while iterating.
Run the full suite before accepting a port, following [Testing](../testing.md).
After sandbox/build-plugin changes, also check packaged extras and the `runIde` sandbox;
tests use a separate bundle.

`verifyPlugin` checks `pluginVerification.ides`. Keep that selection bound to
`pluginSinceBuild` / `pluginUntilBuild` and inspect the actual selected IDEs, especially for EAPs.
Read each `verification-verdict.txt` under `build/reports/pluginVerifier/` and its problem reports.
The task also fails on `INTERNAL_API_USAGES`, so distinguish binary incompatibilities from API
usage findings and record new or changed risks.

Record the artifact, actual platform build, revision, fixture version, test counts and filters,
verifier findings, and unresolved issues in the platform note.

## Platform maintenance releases

An update within the same platform release line normally changes `platformVersion` without
changing the compatibility bounds or synchronizing the plugin's release number.
Still check bundled library alignment and run the full suite. Compare relevant jars with
`shasum -a 256`; inspect changed jars for version differences.

## Keeping the branches in sync

Merge fixes forward from older platform branches into newer ones, preserving the newer target's
versions and API requirements while incorporating incoming refactorings. Review conflicts for
combined behavior instead of taking either side wholesale; see the
[helpers-locator merge example](2026.2.md#merging-inherited-test-infrastructure).
Review reused `git rerere` resolutions too.

After merging, compile and run the JUnit tests plus a feature exercising the changed scaffolding
(such as `implicit_py_symbols_resolve`). Run the full suite before merging the port PR.

## Maintaining these notes

Use one `<year>.<release>.md` file per platform release line, including its maintenance and EAP
findings. Record target builds and issue/PR, revision-qualified validation, changes, rejected
approaches, and remaining issues. Keep historical measurements distinct from current status.

Keep reusable procedures here or in the testing/developer guides. Put platform-specific fixes,
examples, and measurements in the versioned files, linking to shared procedures instead of copying them.
