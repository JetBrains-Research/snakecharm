# AGENTS.md

Guidance for AI coding agents (and human newcomers) working in this repository. Kept
tool-agnostic on purpose — see also `DEVELOPER.md` for the deep parser/lexer walkthrough and
`README.md` for the user-facing feature list.

## What this is

**SnakeCharm** is an IntelliJ Platform plugin (Kotlin) that adds IDE support for the
[Snakemake](https://snakemake.readthedocs.io/) workflow language to PyCharm and other
IntelliJ-based IDEs. It is built **on top of the bundled Python plugin's PSI/API** — most of its
extension points are registered against `language="Python"` and it extends Python parsing rather
than defining a language from scratch.

## Build & test

The Gradle build uses a **JDK 21 toolchain** (`javaVersion` in `gradle.properties`) and the Gradle
version pinned there (`gradleVersion`). **Launch Gradle itself with JDK 21**, not just as an
available toolchain — the pinned Gradle can crash under a much newer JVM with a cryptic error
(Gradle 8.x on JDK 24 fails with `Type T not present`). Set `JAVA_HOME` to a JDK 21 before building
from the CLI and **verify it** with `"$JAVA_HOME/bin/java" -version`: on macOS
`/usr/libexec/java_home -v 21` treats 21 as a *minimum*, so with no JDK 21 installed it returns a
newer JDK, exits 0, and you get the Gradle crash above with no hint why. Use a jenv/asdf/SDKMAN path
(`jenv prefix 21`) or an explicit install path.

```shell
./gradlew buildPlugin      # -> build/distributions/snakecharm-*.zip
./gradlew test             # JUnit + Cucumber suite
./gradlew runIde           # sandbox IDE with the plugin installed
./gradlew verifyPlugin     # IntelliJ Plugin Verifier
```

The target IDE (`platformType`/`platformVersion` in `gradle.properties`) is downloaded
automatically on first build (hundreds of MB). `platformType = PC` is PyCharm Community, `PY` is
PyCharm Professional. Note that **2025.2 is the last standalone PyCharm Community release** — from
2026.1 (build 261) the unified PyCharm ships only under the `PY` artifact.

**Wrappers bundle:** `:buildWrappersBundle` reads `snakemakeWrappersRepoPath` (a local
[snakemake-wrappers](https://github.com/snakemake/snakemake-wrappers) checkout) and runs as part of
`prepareSandbox`, so it sits in front of `buildPlugin`, `runIde` **and** the test tasks. The property
`snakemakeWrappersRepoPath` is unset by default in `gradle.properties` so the plugin bundle and local IDE
run will not provide wrappers related completion and other features until the variable is set in properties
or using cmdline like `./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers`. 
The test-only bundle (`:buildTestWrappersBundle`, what `test` actually consumes) reads by default
`testData/wrappers_storage` and needs no property. 

**CLI build memory:** if `:compileKotlin` dies with `OutOfMemoryError: GC overhead limit exceeded`,
give the Kotlin daemon more heap — append `-Pkotlin.daemon.jvmargs=-Xmx4g` (transforming some large
generated methods can exhaust the default heap).

### Running tests

Tests are **Cucumber/Gherkin** feature files under `src/test/resources/features/**`, executed
through a single JUnit runner, `AllCucumberFeaturesTest` (glue/step definitions in
`src/test/kotlin/features/glue/`). There is no per-feature test class.

- **Run one feature:** add a `@here` tag above its `Feature:` line (or above a single `Scenario:` /
  `Scenario Outline:`) and set `tags = "not @ignore and @here"` in `AllCucumberFeaturesTest.kt`;
  revert both afterwards. Once #577 lands, the runner edit is unnecessary — `test` forwards
  `CUCUMBER_TAGS='@here'` to cucumber's `cucumber.filter.tags`, which overrides the annotation. Worth
  the trouble either way: it turns a 25-minute suite into a ~60-second one.
- **Scenarios share one project.** The light fixture's descriptor is cached (it has to be on 2026.2 —
  a per-scenario mock SDK collides on symbolic id), so project *services* survive into the next
  scenario. A step that wants project state — framework enabled/disabled, settings, SDK — must set it
  explicitly; it cannot rely on a fresh project's defaults. `Given a snakemake with disabled framework
  project` broke exactly this way, and the scenario that depended on it stayed green for years only
  because a second bug happened to cancel it out.
- **`testData` is NOT a declared input of the `test` task.** After editing any feature or
  test-data file, run `./gradlew cleanTest test` — plain `test` may serve stale cached results.
- Test data lives in `testData/`. Snakemake API is mocked per-version under
  `testData/MockPackages3_smk_<version>/snakemake` (and a bare `testData/MockPackages3/snakemake`);
  cucumber steps select one via `Given a snakemake:<version> project`. Only the API files that
  differ between versions are copied into each mock (see `DEVELOPER.md` → Testdata).
- **Fresh-checkout gotcha (saves hours):** `testData/MockPackages3/snakemake` is **gitignored** and
  absent on a clean checkout — the *unversioned* `Given a snakemake project` scenarios (~135) then
  fail because `resolveQualifiedName("snakemake")` returns `[]`, while the checked-in per-version
  mocks (`MockPackages3_smk_<ver>`) still resolve. Provision it (see `DEVELOPER.md` → Configure Tests,
  step 2): symlink` snakemake` to **`src/snakemake`** (https://github.com/snakemake/snakemake, checkout
  desired version using repo tags) to `testData/MockPackages3/snakemake`. 
  **Two traps that make a correct fixture look like it does nothing:** the version must match
  `snakemake_api.yaml`'s `defaultVersion` (e.g. 9.9.0), and the test IDE
  sandbox persists a VFS/index under `.sandbox_pycharm/<ide>/system-test/` that **`cleanTest` doesn't
  clear** — if you add the fixture after a prior run, `rm -rf .sandbox_pycharm/*/system-test` once. If
  you see a wall of `snakemake`-resolution failures on a fresh checkout, suspect this fixture, **not**
  your change. (Full write-up: PR #574.)
- **Analyzing results:** the full suite is large (~3200 scenarios; a full `test` run takes a while —
  prefer the single-feature `@here` recipe while iterating). To triage or diff failures, parse
  `build/test-results/test/TEST-*.xml`: each `<testcase>` with a `<failure>`/`<error>` child is a
  failing scenario (name = `<feature> > <scenario> [#example]`).

## Architecture

Two languages, both layered onto the Python plugin:

1. **Snakemake** (`SnakemakeLanguageDialect`) — the `Snakefile` / `*.smk` / `*.rule(s)` files. Its
   parser (`lang/parser/`) drives the Python `PyParser` API rather than a raw `PsiParser`: the
   lexer/parser flip Snakemake keywords (`rule`, `checkpoint`, …) from Python identifiers to
   Snakemake token types **only outside pure-python blocks** (`run:`/`onstart`/`onsuccess`/
   `onerror`), and delegate everything else to the Python parser. PSI lives in `lang/psi/`
   (`SmkFile`, sections, rules), custom PSI types in `lang/psi/types/`, references in
   `lang/psi/references/`, stubs in `lang/psi/stubs/`.

2. **SmkSL** — the Snakemake String Language embedded in strings like
   `"results/sample_{genome}.bam"`. Lives under `stringLanguage/`, lexer generated from
   `stringLanguage/lang/parser/smk_sl.flex` (JFlex), injected into Python string literals.

SnakemCharm plugin features are based on the sources of the snakemake project 
(https://github.com/snakemake/snakemake) so the plugin tries to do maximum static analysis of the underlying 
snakemake python code. Due to the highly dynamic implementation of a snakemake framework the SnakeCharm 
provides API descriptions of the implicit python api available in different blocks of Snakemake DSL. 
Additionally, API changes among different snakemake versions, so snakemake version is considered 
as `language level`. File `snakemake_api.yaml` (loaded by SnakemakeApiYamlAnnotationsService into project level
`com.jetbrains.snakecharm.codeInsight.SnakemakeApiService` service class) describes API changes among 
different snakemake versions. Key `defaultVersion` (e.g. 9.9.0) sets the default language level for the new 
 projects, and it is the latest language level officially supported by the plugin.

Feature areas (each maps to a source package and a `features/` test dir):

- `lang/highlighter/`, `lang/validation/` — syntax highlighting + annotators (registered against
  Python; some run through `SmkStandardAnnotatorManager` / `SmkDumbAwareAnnotatorManager`).
- `codeInsight/` — completion contributors and resolve for Snakemake magic (`config`, `rules`,
  `rules.<name>.<section>`, wildcards, api methods like `expand`/`temp`, wrapper names). The implicit
  "runtime magic" symbols (`expand`, `temp`, `config`, `rules`, …) are built by
  `SmkImplicitPySymbolsProvider`, which resolves them by qualified name against the project SDK's
  snakemake package.
- `inspections/` — ~56 local inspections for common Snakemake mistakes.
- `framework/` — Snakemake framework detection: locating the `snakemake` package via the project
  SDK / package manager, which gates most features and drives version-specific behaviour.
- `lang/structureView/`, `lang/documentation/`, `lang/formatter/`, `spellchecker/`, `actions/` —
  the corresponding IDE integrations.

Extension points are wired in `src/main/resources/META-INF/plugin.xml` — the fastest way to find
the entry class for any feature is to grep that file.

## Build / platform conventions

- `gradle.properties` is the single source of truth for the target platform: `platformType`,
  `platformVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformBundledPlugins`.
- Plugin version scheme (`pluginVersion`) is `YEAR.MAJOR.MINOR`, where `YEAR.MAJOR` is the
  **minimal compatible platform** and `MINOR` is the plugin build digit. A new `pluginVersion`
  must also get a matching section in `CHANGELOG.md`, or `patchPluginXml` fails.
- Build numbers map to IDE versions per
  [build-number-ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
  (`2025.2`=`252`, `2026.1`=`261`, …). `DEVELOPER.md` → "Update to new Platform API" is the
  checklist for a platform bump.
- **A platform bump moves more than `platformVersion`.** Three toolchain baselines can move with it,
  and each fails *before* your source is even considered, with an error that doesn't name the cause:
  the **Kotlin compiler** must be new enough to read the platform's metadata (a compiler reads
  metadata at most one minor above itself — 2026.2 ships metadata 2.4, so Kotlin 2.2 fails with
  "compiled with an incompatible version of Kotlin"); the **Java toolchain** must match the
  platform's bytecode target (2026.2 emits Java 25, so javac 21 reports "bad class file … wrong
  version 69.0"); and the **`intelliJPlatform` gradle-plugin version** decides whether the Python
  plugin's v2 content modules load *in tests* at all (2.16.0 → 2.18.1 took one port from 3361 failing
  scenarios to 1153). Check all three before debugging your own code.
- **Logged errors are test failures.** `TestLoggerFactory` promotes anything logged at error level to
  a failed scenario, so one benign platform log can fail hundreds of unrelated tests. When triaging a
  wall of failures, group by exception message first — it is usually one cause, not many.
- **Platform-bump gotcha:** since 2025.2+ the platform is modular — APIs, inspections, and extension
  points that used to live in *core* have been split into separate modules / bundled plugins with
  their own classloaders. If a class or EP that worked before goes missing after a bump (often only
  visible in tests), declare it explicitly with `bundledModule("…")` / `bundledPlugin("…")` in
  `build.gradle.kts` and consult the
  [API changes list](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2025.html). (E.g.
  `SpellCheckingInspection` moved from core to the Grazie plugin, `tanvd.grazi`.)
