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
`prepareSandbox`, so it sits in front of `buildPlugin` and `runIde` — but **not** the test tasks,
which route through `prepareTestSandbox` and the separate test bundle below. That
property is commented out in `gradle.properties` by default, so a plain `buildPlugin` / `runIde`
yields a plugin without wrapper completion and the other wrapper-driven features; pass it explicitly
to include them: `./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers` (on
TeamCity it comes from the wrappers VCS root — see issue #571). The test-only bundle
(`:buildTestWrappersBundle`, what `test` actually consumes) defaults to `testData/wrappers_storage`
and needs no property.

**CLI build memory:** if `:compileKotlin` dies with `OutOfMemoryError: GC overhead limit exceeded`,
give the Kotlin daemon more heap — append `-Pkotlin.daemon.jvmargs=-Xmx4g` (transforming some large
generated methods can exhaust the default heap).

### Running tests

Tests are **Cucumber/Gherkin** feature files under `src/test/resources/features/**`, executed
through a single JUnit runner, `AllCucumberFeaturesTest` (glue/step definitions in
`src/test/kotlin/features/glue/`). There is no per-feature test class.

- **Run one feature:** add a `@here` tag above its `Feature:` line (or above a single `Scenario:` /
  `Scenario Outline:`) and set `tags = "not @ignore and @here"` in `AllCucumberFeaturesTest.kt`;
  revert both afterwards. If PR #577 lands, the runner edit becomes unnecessary — `test` there
  forwards `CUCUMBER_TAGS='@here'` to cucumber's `cucumber.filter.tags`, which overrides the
  annotation. Worth the trouble either way: it turns a 25-minute suite into a ~60-second one.
- **Scenario isolation is thinner than it looks.** Every scenario asks IntelliJ's light-fixture
  framework for a test project by handing it a `LightProjectDescriptor` — the object that says
  which Python SDK and library roots the project needs. The framework hands back the *same* project
  as long as it is given the same descriptor, and rebuilds it when the descriptor changes. On `master`
  `StepDefs` constructs a fresh descriptor per scenario, so scenarios are mostly insulated from each
  other by accident. #577 has to cache descriptors instead — on 2026.2 an SDK is a workspace-model
  entity, so building a second mock SDK with the same name logs "symbolic id already exists", which
  `TestLoggerFactory` turns into ~1070 failed scenarios. Once the project is shared, everything held
  by a project-level *service* — framework enabled/disabled, settings, the configured SDK — survives
  into the next scenario. **Write steps that set the project state they need rather than assume a
  fresh project's defaults.** `Given a snakemake with disabled framework project` is the cautionary
  example: it never disabled anything, it only skipped the enabling, and it passed for years purely
  because each scenario used to start from a clean project.
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
  step 2): point `testData/MockPackages3/snakemake` at the `src/snakemake` package of a
  [snakemake](https://github.com/snakemake/snakemake) checkout, at the release tag you want.
  **Two traps that make a correct fixture look like it does nothing:** the checkout must be at the
  version `snakemake_api.yaml` declares as `defaultVersion` (currently 9.9.0), and the test IDE
  sandbox persists a VFS/index under `.sandbox_pycharm/**/system-test/` that **`cleanTest` doesn't
  clear** — after adding the fixture to an already-tested checkout, remove it once with
  `find .sandbox_pycharm -maxdepth 3 -name system-test -exec rm -rf {} +` (its depth varies with
  how the tests were launched, so a fixed glob can silently match nothing). If you see a wall of
  `snakemake`-resolution failures on a fresh checkout, suspect this fixture, **not** your change.
  (Full write-up: PR #574.)
- **Analyzing results:** the suite is large — ~3250 Cucumber scenarios plus ~170 plain JUnit tests,
  around 25 minutes for a full `test` run, so prefer the single-feature `@here` recipe while
  iterating. To triage or diff failures, parse
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

Plugin features are derived from the sources of the
[snakemake project](https://github.com/snakemake/snakemake), so SnakeCharm does as much static
analysis of the underlying snakemake Python code as it can. Because the framework itself is highly
dynamic, the plugin additionally ships descriptions of the implicit Python API available in each
block of the Snakemake DSL. That API changes between snakemake releases, so the snakemake version is
treated as a **language level**: `snakemake_api.yaml` at the repo root (loaded by
`SnakemakeApiYamlAnnotationsService` into the project-level
`com.jetbrains.snakecharm.codeInsight.SnakemakeApiService`) records the differences between
versions. Its `defaultVersion` key (currently 9.9.0) is the language level new projects get, and the
latest one the plugin officially supports. Additionally, users could adjust `snakemake_api.yaml` for 
already installed SnakeCharm, e.g. in macOS this file path will be: 
`~/Library/Application Support/JetBrains/PyCharm2025.3/plugins/snakecharm/extra/snakemake_api.yaml` 

Feature areas (each maps to a source package and a `features/` test dir):

- `lang/highlighter/`, `lang/validation/` — syntax highlighting + annotators (registered against
  Python; some run through `SmkStandardAnnotatorManager` / `SmkDumbAwareAnnotatorManager`).
- `codeInsight/` — completion contributors and resolve for Snakemake magic (`config`, `rules`,
  `rules.<name>.<section>`, wildcards, api methods like `expand`/`temp`, wrapper names). The implicit
  "runtime magic" symbols (`expand`, `temp`, `config`, `rules`, …) are built by
  `SmkImplicitPySymbolsProvider`, which resolves them by qualified name against the project SDK's
  snakemake package.
- `inspections/` — ~45 local inspections (`<localInspection>` entries in `plugin.xml`) for common
  Snakemake mistakes.
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
  tests, of ~3400, down to 1153). Check all three before debugging your own code.
- **Logged errors are test failures.** `TestLoggerFactory` promotes anything logged at error level to
  a failed scenario, so one benign platform log can fail hundreds of unrelated tests. When triaging a
  wall of failures, group by exception message first — it is usually one cause, not many.
- **Platform-bump gotcha:** since 2025.2 the platform is modular — APIs, inspections, and extension
  points that used to live in *core* have been split into separate modules / bundled plugins with
  their own classloaders. If a class or EP that worked before goes missing after a bump (often only
  visible in tests), declare it explicitly with `bundledModule("…")` / `bundledPlugin("…")` in
  `build.gradle.kts` and consult the
  [API changes list](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2025.html). (E.g.
  `SpellCheckingInspection` moved from core to the Grazie plugin, `tanvd.grazi`.)
