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
from the CLI (e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`, or a jenv/asdf/SDKMAN shim);
`.java-version` also pins 21.

```shell
./gradlew buildPlugin      # -> build/distributions/snakecharm-*.zip
./gradlew test             # JUnit + Cucumber suite
./gradlew runIde           # sandbox IDE with the plugin installed
./gradlew verifyPlugin     # IntelliJ Plugin Verifier
```

The target IDE (`platformType`/`platformVersion` in `gradle.properties`) is downloaded
automatically on first build (hundreds of MB). `platformType = PC` is PyCharm Community, `PY` is
PyCharm Professional.

Many test tasks need the wrappers repo path: append
`-PsnakemakeWrappersRepoPath=testData/wrappers_storage`.

### Running tests

Tests are **Cucumber/Gherkin** feature files under `src/test/resources/features/**`, executed
through a single JUnit runner, `AllCucumberFeaturesTest` (glue/step definitions in
`src/test/kotlin/features/glue/`). There is no per-feature test class.

- **Run one feature:** add a `@here` tag above its `Feature:` line and set
  `tags = "not @ignore and @here"` in `AllCucumberFeaturesTest.kt`; revert both afterwards.
- **`testData` is NOT a declared input of the `test` task.** After editing any feature or
  test-data file, run `./gradlew cleanTest test` — plain `test` may serve stale cached results.
- Test data lives in `testData/`. Snakemake API is mocked per-version under
  `testData/MockPackages3_smk_<version>/snakemake` (and a bare `testData/MockPackages3/snakemake`);
  cucumber steps select one via `Given a snakemake:<version> project`. Only the API files that
  differ between versions are copied into each mock (see `DEVELOPER.md` → Testdata).
- **Fresh-checkout gotcha (saves hours):** `testData/MockPackages3/snakemake` is **gitignored** and
  absent on a clean checkout — you must clone the snakemake repo and symlink it there (see
  `DEVELOPER.md` → Configure Tests, step 2). Without it, a large batch (~100+) of resolve/completion
  scenarios for the *unversioned* `snakemake` project fail — `resolveQualifiedName("snakemake")`
  returns `[]` — while the checked-in per-version mocks (`MockPackages3_smk_<ver>`) still resolve. If
  you see a wall of `snakemake`-resolution failures on a fresh checkout, suspect this missing
  fixture, **not** your change.
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

Feature areas (each maps to a source package and a `features/` test dir):

- `lang/highlighter/`, `lang/validation/` — syntax highlighting + annotators (registered against
  Python; some run through `SmkStandardAnnotatorManager` / `SmkDumbAwareAnnotatorManager`).
- `codeInsight/` — completion contributors and resolve for Snakemake magic (`config`, `rules`,
  `rules.<name>.<section>`, wildcards, api methods like `expand`/`temp`, wrapper names).
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
- **Platform-bump gotcha:** since 2025.2+ the platform is modular — APIs, inspections, and extension
  points that used to live in *core* have been split into separate modules / bundled plugins with
  their own classloaders. If a class or EP that worked before goes missing after a bump (often only
  visible in tests), declare it explicitly with `bundledModule("…")` / `bundledPlugin("…")` in
  `build.gradle.kts` and consult the
  [API changes list](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2025.html). (E.g.
  `SpellCheckingInspection` moved from core to the Grazie plugin, `tanvd.grazi`.)
