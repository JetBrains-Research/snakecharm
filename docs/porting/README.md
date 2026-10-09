# Porting SnakeCharm across IntelliJ Platform releases

Use this checklist before changing the platform, Java, Kotlin, or IntelliJ Platform Gradle Plugin.
For ordinary setup and building, see [DEVELOPER.md](../../DEVELOPER.md); for all test setup and
execution, see [Testing](../testing.md).

## Port history

Read the relevant source and target platform notes before starting a port or removing a workaround.
These files record findings at specific revisions and builds; intermediate failure counts and
rejected experiments are historical evidence, not the current test baseline.

| Platform release line | Platform build | Engineering notes |
|---|---|---|
| 2026.1 | 261 | [Unified PyCharm and Python API migration](2026.1.md) — PR [#570](https://github.com/JetBrains-Research/snakecharm/pull/570) |
| 2026.2 | 262 | [Annotators, runtime libraries, fixtures, and sandbox wiring](2026.2.md) — PR [#577](https://github.com/JetBrains-Research/snakecharm/pull/577) |
| 2026.3 | 263 | [EAP API and completion changes](2026.3.md) — issue [#596](https://github.com/JetBrains-Research/snakecharm/issues/596) |

## Platform-update checklist

### Establish the target and baseline

1. Read the current `gradle.properties`, `gradle/libs.versions.toml`, `.java-version`,
   and the relevant port history. Capture the current test and verifier baseline.
2. Select the target IDE. Build numbers map to releases per
   [JetBrains' build-number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html):
   2025.2 → 252, 2026.1 → 261, 2026.2 → 262, etc.
   Standalone PyCharm Community ended with 2025.2; later PyCharm releases use `platformType = PY`.
3. Check available versions with `./gradlew printProductsReleases`. It lists only its configured
   channels (currently RELEASE and EAP); a channel-limited result is not proof the target is latest.
   Released versions, build numbers, and dates can also be checked through
   [JetBrains' releases API](https://data.services.jetbrains.com/products/releases?code=PY&type=release&latest=false).
4. Before changing source, verify the **existing plugin binary** against the new IDE.
   Run the Plugin Verifier with that artifact and target explicitly, preserving its reports.
   Running this repository's `verifyPlugin` unmodified selects its configured artifact/IDE set;
   ensure those are the ones being compared.
   `NoSuchFieldError` / `NoSuchMethodError` findings form a binary-incompatibility worklist.
   Some changes, such as retyped protected fields, are not exposed by compilation alone.

### EAP and snapshot targets

For an unreleased platform, use a version listed in the
[IntelliJ snapshots metadata](https://www.jetbrains.com/intellij-repository/snapshots/com/jetbrains/intellij/pycharm/pycharmPY/maven-metadata.xml),
such as `263-EAP-SNAPSHOT`. A `-SNAPSHOT` target makes `build.gradle.kts` set
`useInstaller = false`, resolving the repository artifact instead of an installer.

A release-line snapshot moves when new EAPs arrive. Pin a concrete snapshot, such as
`263.6259.38-EAP-SNAPSHOT`, for comparable runs. Record the actual build from the downloaded
IDE's `build.txt`, not only the moving snapshot name.

### Align toolchains and bundled libraries

Check these before debugging source or widespread test failures:

| Setting | Required check |
|---|---|
| `kotlin` in `gradle/libs.versions.toml` | Compiler can read target metadata; for example, Kotlin 2.2 could not read the 2026.2 platform's 2.4 metadata |
| `javaVersion` and `.java-version` | Both match the required bytecode/toolchain baseline; launch Gradle on that JDK too |
| `gradleVersion` and Gradle wrapper | Support that JDK and the build plugins |
| `intelliJPlatform` in the version catalog | Supports the target's modules, test runtime, and sandbox task layout |
| `kotlinPlatform`, `kotlinxSerializationPlatform` | Match the libraries shipped by the target IDE |

`intelliJPlatform` is the **Gradle plugin version**, not the platform version.
Consult its [documentation](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
and [plugin template](https://github.com/JetBrains/intellij-platform-plugin-template).
Review related tooling such as Qodana when updating the build.

A toolchain declaration alone does not select the Gradle daemon's JDK: `instrumentCode` loads
platform classes inside that daemon. For example, Java 25 platform classes (major version 69)
cannot load into a Java 21 daemon. Follow [JDK setup](../../DEVELOPER.md#environment-setup)
and pass `-Dorg.gradle.java.installations.paths="$JAVA_HOME"` if discovery needs help.

The IntelliJ Platform Gradle Plugin also determines whether Python's v2 content modules load in
tests. The 2026.2 port's 2.16.0 → 2.18.1 change reduced thousands of failures before source fixes.
Later sandbox and IDE-import changes are documented in [2026.2](2026.2.md).

Read runtime library versions from the downloaded IDE rather than guessing:

```shell
unzip -p <ide>/lib/intellij.libraries.kotlinx.serialization.core.jar META-INF/MANIFEST.MF | grep Implementation-Version
# On the recorded platforms, Kotlin is merged into util-8.jar without a versioned manifest.
# Read the three constructor integers, e.g. 2 / 4 / 20 -> 2.4.20.
javap -p -c -cp <ide>/lib/util-8.jar kotlin.KotlinVersionCurrentValue | sed -n '/KotlinVersion get/,/areturn/p'
```

These jar paths describe the recorded platforms; check the new distribution if its layout changes.
`build.gradle.kts` forces the catalog's library versions onto runtime classpaths.
The flat Gradle test classpath can let our dependencies shadow the platform's libraries.
A stale stdlib can cause `@DebugMetadata` mismatch; stale serialization can cause
`AbstractMethodError` in `PluginGeneratedSerialDescriptor.kt`.
One logged error may fail hundreds of scenarios. See
[the serialization investigation](2026.2.md#serialization-abi) and
[test failure clustering](../testing.md#cluster-failure-messages-first).

### Update compatibility and source

- Set `platformVersion`, `pluginSinceBuild`, and `pluginUntilBuild` deliberately.
  Use the newest platform line actually built and tested for the upper bound.
  Do not widen the manifest merely to make an incompatible binary install.
- Follow [plugin versioning](../../AGENTS.md#plugin-versioning) and the
  [release checklist](../../DEVELOPER.md#release-checklist) when changing `pluginVersion`.
  A newer build/test target does not automatically change the minimum supported platform.
- Since 2025.2, APIs and extension points have moved into separate modules/bundled plugins.
  If a previously available class or EP disappears, check whether an explicit
  `bundledModule("…")` / `bundledPlugin("…")` is needed. For example, spellchecker was
  extracted from core and `SpellCheckingInspection` moved to Grazie (`tanvd.grazi`).
  Consult the target year's [API changes](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html).
- Check replacement APIs' class-level status as well as method deprecation.
  A behavior-equivalent internal API can still worsen compatibility risk.
- Preserve the PythonCore API boundary: unified PyCharm exposes Professional-only APIs at
  compile time, while SnakeCharm also supports IDEA with the community Python plugin.
  See [architecture](../architecture.md#platform-api-boundaries).
- Review wrapper metadata updates and their separate TeamCity configuration under
  [build and packaging](../../DEVELOPER.md#build-and-packaging).

## Verification and acceptance

Compile production and test sources, build the distributable, and run focused tests while iterating.
Run the full suite before accepting a platform port, following [Testing](../testing.md).
After sandbox/build-plugin changes, also check the packaged extras and `runIde` sandbox:
passing tests use a separate bundle and do not establish production packaging correctness.

`verifyPlugin` checks the IDEs in `pluginVerification.ides`, not whatever the manifest claims.
Keep its selection bound to `pluginSinceBuild` / `pluginUntilBuild`; do not reintroduce a
hardcoded wider range. Check the actual selected IDEs, especially for EAP targets.
A wildcard such as `261.*` does match real 261 builds (one recorded run selected
`PY-261.27258.51`); inspect reports before changing selection based on assumptions.

Read each `verification-verdict.txt` under `build/reports/pluginVerifier/` and the accompanying
problem reports. The task also fails on `INTERNAL_API_USAGES`, including long-standing ones,
so a nonzero exit alone does not identify a binary incompatibility. Record new or changed API
usages and remaining risks alongside the compatibility verdict.

Record the tested artifact, actual platform build, revision, fixture version, test counts and
filters, verifier findings, and unresolved issues in the platform note. Keep the final validation
snapshot distinct from intermediate experiments.

## Platform maintenance releases

A platform update such as 2026.2.1 → 2026.2.2 normally changes `platformVersion` only;
the release line remains 262, so the existing compatibility bounds need not change.
The plugin's third version component is independent of this maintenance number.

Still check bundled stdlib/serialization alignment and run the full suite. Compare relevant jars
between downloaded IDEs with `shasum -a 256`; byte-identical jars need no further version
investigation. If a jar changed, inspect its contents/version. The recorded 2026.2.2 update
passed at the same test count without source changes; that is evidence for that update, not a
guarantee for future maintenance releases.

## Keeping the branches in sync

When maintaining stacked port branches, merge fixes **forward** from the older platform branch
into the newer one (for example, 2026.1 → 2026.2). Keep the older branch's supported API baseline
intact; do not merge newer-platform API requirements back into it.

**The trap is a conflict where the newer branch has independently grown a *superset* of what the
older one is refactoring.** Taking either side whole then silently drops half the behaviour, and the
loss does not show up as a conflict marker or a compile error — only as a suite that fails
somewhere unrelated. The worked example: the Python helpers-locator workaround. 2026.2 had extended
it to handle *two* platform shapes (prune the crashing Pro locator where the EP exists, register the
EP outright where it does not, which is the 2026.2 case) while 2026.1 had moved the 2026.1-only half
out of the vendored `PythonMockSdk` into `SmkTestPythonHelpersLocatorFix`. Neither side was
"correct": the answer was the newer branch's *behaviour* inside the older branch's *structure*.

So the resolution rule is **newer branch's values, older branch's structure** — versions, platform
constants and platform-specific behaviour come from the branch being merged *into*; refactorings,
extractions and comments come from the branch being merged *from*. Check every conflict against it
explicitly rather than reaching for `--ours`/`--theirs`.

`git rerere` (a per-user git setting, not repo config — `git config --global rerere.enabled true`)
replays a conflict resolution you have already made, which is worth enabling before the first of
these merges. It is a reason to get the resolution right once, not a reason to skip reading it.

After merging, at minimum: compile, then run the JUnit tests plus a feature that builds the mock SDK
(`implicit_py_symbols_resolve` is a good choice — every scenario exercises the merged test
scaffolding). A full run is still owed before the PR merges.

## Maintaining these notes

Use one file per platform release line, `<year>.<release>.md`, with maintenance releases and
EAP findings inside it. Include the target builds and issue/PR, a dated or revision-qualified
validation snapshot, build changes, API changes, test infrastructure findings, rejected approaches,
and remaining issues. Preserve evidence for failed approaches so it is not needlessly repeated.

Promote reusable procedures into this checklist or the testing/developer guides and link to them.
Use descriptive cross-file links instead of “above” or a bare item number from another port.
Historical versions, failure counts, and unresolved statuses must be labeled as such.
