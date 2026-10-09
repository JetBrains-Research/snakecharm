# Developer guide

This guide covers environment setup, building, packaging, and releasing SnakeCharm.
For tests, including first-time fixture setup, use [Testing](docs/testing.md).
For source structure and parser internals, use [Architecture](docs/architecture.md).
For platform or toolchain upgrades, follow the [porting checklist](docs/porting/README.md).

## Environment setup

Clone the repository and import it in IntelliJ IDEA as a Gradle project
(`File | New | Project From Existing Sources...`).

The required JDK is specified by `javaVersion` in `gradle.properties` and mirrored in
`.java-version`. The Gradle wrapper version is tracked by `gradleVersion` and
`gradle/wrapper/gradle-wrapper.properties`. Use the checked-in wrapper.

Launch **Gradle itself** on the required JDK, not just its compilation toolchain. A JDK that is
too new can break the pinned Gradle; one that is too old cannot load platform classes during
`instrumentCode`. Historical symptoms include `Type T not present` and
`UnsupportedClassVersionError`, neither of which makes the configuration mistake obvious.

With jenv, `.java-version` selects the branch's JDK once it is installed and registered.
asdf reads it only with `legacy_version_file = yes` in `~/.asdfrc`; otherwise it reads
`.tool-versions`. Recheck the active JDK after switching branches. Alternatively, set
`JAVA_HOME` to an explicit installation path:

```shell
# Run from the repository root. Install this exact version if needed.
JDK=$(cat .java-version)
brew install "openjdk@$JDK"                  # macOS / Homebrew
export JAVA_HOME=$(jenv prefix "$JDK")       # if registered with jenv
# Or set JAVA_HOME to your explicit JDK installation.
"$JAVA_HOME/bin/java" -version              # verify the version before running Gradle
```

On macOS, do not rely on `/usr/libexec/java_home -v <n>` to pin an exact version: it can
return a newer installed JDK and exit successfully. If Gradle cannot discover a jenv-managed
toolchain, pass `-Dorg.gradle.java.installations.paths="$JAVA_HOME"` as well.
Set the IDE's Gradle JVM to the same required JDK.

## Build and packaging

```shell
./gradlew buildPlugin      # build/distributions/snakecharm-*.zip
./gradlew runIde           # sandbox IDE with the plugin installed
./gradlew verifyPlugin    # compatibility reports; see the porting guide for interpretation
```

`platformType` and `platformVersion` in `gradle.properties` select the target IDE, downloaded
automatically on the first build (hundreds of MB). PyCharm was unified in 2025.1; 2025.2 was the
last standalone Community release. From 2025.3 onward, use the unified `PY` artifact.
Free/Pro licensing is a runtime state, not a different downloaded SDK.

If `:compileKotlin` fails with `OutOfMemoryError: GC overhead limit exceeded`, append
`-Pkotlin.daemon.jvmargs=-Xmx4g`. This increases the **Kotlin daemon** heap, not the Gradle
daemon or test JVM heap.

### Wrapper metadata

Local builds may omit wrapper metadata. With `snakemakeWrappersRepoPath` unset or blank,
`:buildWrappersBundle` skips with a warning and the plugin builds without wrapper completion
and other wrapper-driven features. A nonblank invalid path fails the build.

To include metadata, provide a local
[snakemake-wrappers](https://github.com/snakemake/snakemake-wrappers) checkout whose contents
match `snakemakeWrappersRepoVersion`:

```shell
./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers
```

The same property applies to `runIde`; it can also be set locally in `gradle.properties`.
The property does not check out the requested revision for you.

When `TEAMCITY_VERSION` is present, unset or blank paths fail any task graph containing
`:buildWrappersBundle`. TeamCity configurations that build the plugin must pass the wrappers
VCS-root checkout through `-PsnakemakeWrappersRepoPath=...` (or `testData/wrappers_storage` for
test configurations that also build the plugin). Those configurations live on JetBrains'
TeamCity server. Updating `snakemakeWrappersRepoVersion` locally does not update the CI VCS
root or build parameters; update both as described in `gradle.properties` (issue #571).

Tests use `prepareTestSandbox` and `:buildTestWrappersBundle`, which reads
`testData/wrappers_storage` without this property. They do not run the production bundle task.

### Sandbox wiring invariants

Read this section before changing wrapper tasks or sandbox copying in `build.gradle.kts`:

- Configure all production sandbox producers with
  `withType<PrepareSandboxTask>().configureEach`, excluding `testSandbox`.
  Since IntelliJ Platform Gradle Plugin 2.19.0, `runIde` has its own sandbox tasks;
  configuring only the literal `prepareSandbox` misses them.
- Copy with `from(named("buildWrappersBundle"))` and retain the task's `outputs.file(...)`.
  A plain file provider carries no task dependency and can silently omit the bundle (#588, #591).
- Within production sandbox configuration, keep that `from(...)` unconditional with respect
  to the wrappers path. Otherwise an unset path removes the task from the graph and its
  `onlyIf` warning never runs.
- Retain `outputs.upToDateWhen { false }`: the crawler reads an external checkout, and skipping
  it risks a stale bundle.
- The `onlyIf` path for an unset property deletes any previous bundle so it cannot be shipped.
  Keep that cleanup; a conditional copy alone does not solve stale output.
- Keep production extras out of test sandboxes. Tests read `snakemake_api.yaml` from the
  project directory and use their separate test bundle.

See [the 2026.2 port](docs/porting/2026.2.md#sandbox-wiring) for the sandbox-task change.

## Testing

Follow [Testing](docs/testing.md) for prerequisites, fixture provisioning, Gradle and IDE runs,
and result verification. It also covers targeted runs and the distinction between
`cleanTest` and clearing the sandbox VFS.

## Platform updates

Follow the [porting checklist](docs/porting/README.md) before changing platform or toolchain
versions. It covers binary verification, bundled library alignment, EAP targets, and testing.
Record platform-specific findings in the corresponding `docs/porting/<version>.md`.

## Release checklist

1. Set `pluginVersion` in `gradle.properties` according to
   [the versioning rule](AGENTS.md#plugin-versioning): the minimum platform release line plus
   an independent plugin release number starting at 1.
2. Check `pluginSinceBuild` / `pluginUntilBuild` against the supported and verified IDEs.
   Changing `platformVersion` alone does not change the advertised compatibility range.
3. Add or update the matching section in `CHANGELOG.md`. The build selects
   `getOrNull(pluginVersion) ?: getUnreleased()` for marketplace change notes.
   Keep the explicit versioned section even though there is a fallback. Other versioned
   sections are not included: fold changes from an older still-unreleased section into the
   release that will actually ship when that older release is superseded.
4. Check `pluginPreReleaseSuffix` and the resulting publication channel. An empty suffix
   publishes to the default channel; `-eap` / `-eap.2` selects EAP. The CI build counter is
   separate from the three-component `pluginVersion`.
5. Confirm wrapper metadata and CI configuration, build the distributable, run the
   [required tests](docs/testing.md), and inspect the
   [verifier reports](docs/porting/README.md#verification-and-acceptance).
6. Publish with `./gradlew publishPlugin` using the configured publishing credentials.

## Development resources

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [IntelliJ plugin template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Kotlin and Gradle](https://kotlinlang.org/docs/gradle.html)
- [Snakemake workflow examples](https://github.com/snakemake-workflows/docs)
