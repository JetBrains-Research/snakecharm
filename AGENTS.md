# AGENTS.md

Guidance for AI coding agents (and human newcomers) working in this repository. Kept
tool-agnostic on purpose — see also `DEVELOPER.md` for the deep parser/lexer walkthrough,
`PORTING.md` for what each IntelliJ Platform bump broke and why, and `README.md` for the
user-facing feature list.

## What this is

**SnakeCharm** is an IntelliJ Platform plugin (Kotlin) that adds IDE support for the
[Snakemake](https://snakemake.readthedocs.io/) workflow language to PyCharm and other
IntelliJ-based IDEs. It is built **on top of the bundled Python plugin's PSI/API** — most of its
extension points are registered against `language="Python"` and it extends Python parsing rather
than defining a language from scratch.

## Build & test

The Gradle build uses the JDK toolchain `javaVersion` names in `gradle.properties`, and
`.java-version` in the repo root carries the same number — read it there rather than trusting any
number written in prose, because it moves with the platform and differs per branch. **Launch Gradle
itself on that JDK**, not merely as an available toolchain, because the window is bounded at both
ends: too new and the pinned Gradle crashes with a cryptic error (Gradle 8.x on JDK 24 fails with
`Type T not present`), too old and `instrumentCode` dies loading platform classes (2026.2 emits Java
25, so a JDK 21 daemon gets `UnsupportedClassVersionError: … class file version 69.0`). Set
`JAVA_HOME` before building from the CLI and **verify it** with `"$JAVA_HOME/bin/java" -version`: on
macOS `/usr/libexec/java_home -v <n>` treats `<n>` as a *minimum*, so it can hand back something
newer, exit 0, and leave you with one of those two errors and no hint why. Use a jenv/asdf/SDKMAN
path (`jenv prefix "$(cat .java-version)"`) or an explicit install path.

```shell
./gradlew buildPlugin      # -> build/distributions/snakecharm-*.zip
./gradlew test             # JUnit + Cucumber suite
./gradlew runIde           # sandbox IDE with the plugin installed
./gradlew verifyPlugin     # IntelliJ Plugin Verifier
```

The target IDE (`platformType`/`platformVersion` in `gradle.properties`) is downloaded
automatically on first build (hundreds of MB). `platformType = PC` is PyCharm Community, `PY` is
PyCharm Professional. Note that **2025.2 is the last standalone PyCharm Community release** — from
2025.3 on the unified PyCharm ships only under the `PY` artifact.

**Wrappers bundle:** `:buildWrappersBundle` reads `snakemakeWrappersRepoPath` (a local
[snakemake-wrappers](https://github.com/snakemake/snakemake-wrappers) checkout) and, when that
property is set, runs as part of `prepareSandbox`, so it sits in front of `buildPlugin` and
`runIde` — but **not** the test tasks,
which route through `prepareTestSandbox` and the separate test bundle below. That
property is commented out in `gradle.properties` by default, so a plain `buildPlugin` / `runIde`
yields a plugin without wrapper completion and the other wrapper-driven features; pass it explicitly
to include them: `./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers` (on
TeamCity it comes from the wrappers VCS root — see issue #571). The test-only bundle
(`:buildTestWrappersBundle`, what `test` actually consumes) defaults to `testData/wrappers_storage`
and needs no property.

`prepareSandbox` reaches that bundle through `from(named("buildWrappersBundle"))`, which works only
because the task declares the file as `outputs.file(...)` — `from(<file provider>)` carries no task
dependency, and dropping the dependency produces a wrapper-less plugin *silently*, which has
happened twice (#588, #591). Two things not to "tidy up" there, both of which have been tried and
reverted: the `from(...)` must stay **unconditional**, or `buildWrappersBundle` leaves the task graph
when `snakemakeWrappersRepoPath` is unset and its `onlyIf` — the only place the "no wrappers bundled"
warning is logged — never runs; and `outputs.upToDateWhen { false }` must stay, because declaring
the wrappers checkout as an input is what lets Gradle skip the crawler and ship a stale bundle. The
`onlyIf` deletes any bundle an earlier run left behind; that is what keeps a stale one out, not a
gate around the copy.

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
  (Full write-up: PR #574.) Clearing it makes the next run re-index from scratch, so clear it when
  the fixture actually changed rather than as a routine "start clean" — though the timing table
  below shows the cost is smaller than that warning once implied.
- **A "missing" highlight may only be *demoted*.** `When I check highlighting <level>s` calls
  `CodeInsightTestFixture.checkHighlighting`, which reports only the requested severity (plus
  errors) and *silently discards the rest* — so a highlight whose severity dropped from `WARNING`
  to `WEAK WARNING` fails with the exact same `missing (…)` message as one that is not produced at
  all. Before hunting for a suppression, dump what is actually there: add a temporary step calling
  `fixture.doHighlighting()` and print each `HighlightInfo`'s `severity`, `type`, range,
  `description` and `inspectionToolId`. One run replaces a sandbox debugging session — that is how
  #584 was resolved. Platform bumps move these mappings: on 2026.1
  `ProblemHighlightType.LIKE_UNKNOWN_SYMBOL` renders as `HighlightInfoType.INFO` (weak warning),
  where 2025.2 gave a plain warning. **Fixing such a scenario by re-labelling its step costs
  coverage**, for the same reason: moving `warning`s to `weak warning`s stops it asserting anything
  at WARNING level, and in a scenario without `ignoring extra highlighting` that assertion was the
  guard against stray warnings. Use `I check highlighting warnings and weak warnings`, which asks
  for both.
- **How long a full run takes.** Every figure below is a single measurement of all 3419 tests on
  2026.1, so read the band, not the ordering — these differ by machine and load as much as by what
  they are nominally measuring:

  | run | time |
  |---|---|
  | warm Gradle daemon | ~25 min |
  | cold daemon, sandbox VFS intact | 1h48m; ~95 min extrapolated from an earlier partial run |
  | cold daemon, straight after clearing the sandbox VFS | 1h24m |
  | memory-constrained machine, swapping | 3h52m |

  So a cold full run is **1.5–2 hours**, and clearing the VFS has never actually been measured
  costing more than not clearing — don't clear it routinely (see above), but don't expect the
  timing to tell you whether you did. The one genuinely different regime is swapping: that 3h52m
  was a 16 GB laptop with several GB of swap in use, GC healthy throughout, nothing failing, just
  slow. Check `sysctl vm.swapusage` before concluding anything from a long run. Prefer the
  single-feature `@here` recipe while iterating either way.
- **A platform bump can move a check between inspections, and the scenario then passes vacuously.**
  `Given <X> inspection is enabled` fails loudly on an inspection that was *renamed*
  (`fail("Unknown inspection:…")`), but says nothing when the inspection still exists and merely
  stopped owning the diagnostic the scenario is about. The check for `expand(" ", **1)` moved from
  `PyArgumentListInspection` to `PyTypeCheckerInspection` in 2026.2, which is why one scenario lost
  its warning — and why its sibling, which asserts `expand(" ", **wildcards)` produces *no* warning,
  went on passing while guarding nothing at all. Trace the message to its owner rather than guessing:
  grep the message text in the platform's `messages/*.properties` for its bundle key, then grep the
  extracted plugin jars for the class that references that key, in both the old and new IDE. Two
  greps beat a type-inference theory — the key was renamed
  `INSP.expected.dict.got.type` → `INSP.type.checker.unpack.expected.mapping`, which names the new
  owner outright. A scenario asserting "no warning" is worth re-checking after any bump for exactly
  this reason.
- **Analyzing results:** the suite is large — ~3250 Cucumber scenarios plus ~170 plain JUnit tests.
  Gradle prints each failing scenario and a `N tests completed, M failed` summary, so tee
  the log and reduce it rather than parsing anything: `sed -n '/ > /s/ FAILED$//p' log | sort -u`
  gives a sorted list you can `diff` between two runs (the `/ > /` address skips Gradle's own
  `> Task :test FAILED`). Check the line count against `M failed`. See
  DEVELOPER.md → "Reading test results". The JUnit XML under `build/test-results/test/` holds the
  same information if you need a run whose console output you no longer have — but note it is
  written when the `test` task *ends*, and on an all-green run there is no `N tests completed`
  line either (Gradle prints that only on failure), so **a run in progress looks identical to a
  hung one** — and so does one that ran nothing. `BUILD SUCCESSFUL` says only that no test failed,
  never how many ran, and `CUCUMBER_TAGS` makes an empty run easy to reach: a tag expression
  matching no scenario exits 0 just as loudly as a full green suite. Read the count out of the XML
  (`<testsuite tests="…">`) before believing a green run; a full suite is **3420** across 125 suites (measured on `23097522`, the 2026.2 branch; it was 3419 until the #570 merge added a scenario, so older notes say that). The live signals are the test JVM's accumulating CPU time (`ps -o time=`) and the
  mtime of `build/test-results/test/binary/in-progress-results-generic.bin`; `jstat -gc` tells you
  whether a quiet stretch is a slow scenario or a GC death spiral.

  The same "no summary line" quirk means **a truncated run looks identical to a good one**: an
  all-green `BUILD SUCCESSFUL` says nothing about how many tests ran, so confirm the count from the
  XML (`tests=` summed over `build/test-results/test/*.xml`; it should be 3420) before reporting a
  run as green. A stray `@here` tag or a leftover `tags = "not @ignore and @here"` in
  `AllCucumberFeaturesTest` is the usual cause.

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
latest one the plugin officially supports.

Feature areas (each maps to a source package and a `features/` test dir):

- `lang/highlighter/`, `lang/validation/` — syntax highlighting + annotators (registered against
  Python; some run through `SmkStandardAnnotatorManager` / `SmkDumbAwareAnnotatorManager`). Since
  2026.2 removed `PyAnnotator`, these are `PyElementVisitor`s that take their `PyAnnotationHolder`
  at construction, so they cannot be singletons — and `Annotator.annotate()` is a **per-element**
  callback, so anything built inside it is built once per PSI element per highlighting pass. Guard
  on the containing file first, then cache per `AnnotationHolder.currentAnnotationSession`. Both
  halves of that have been missed once each (`PORTING.md` → "2026.2", item 14).
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
  must also get a matching section in `CHANGELOG.md`, or `patchPluginXml` fails. **Only that
  section ships.** `changeNotes` is `getOrNull(pluginVersion)` (`build.gradle.kts`), so an older
  still-unreleased section sitting below the current one renders nowhere — its fixes go out inside
  the new release with no marketplace change note naming them. Fold any such section into the one
  being released rather than leaving it in place.
- Build numbers map to IDE versions per
  [build-number-ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html)
  (`2025.2`=`252`, `2026.1`=`261`, …). `DEVELOPER.md` → "Update to new Platform API" is the
  checklist for a platform bump.
- **`verifyPlugin` verifies whatever `pluginVerification.ides` lists — not what the manifest claims.**
  Raising `pluginSinceBuild` does not narrow it. That list is bound to
  `pluginSinceBuild`/`pluginUntilBuild` in `build.gradle.kts` so a bump carries the verifier with it;
  don't re-hardcode a range there or the task starts failing against IDEs that can no longer install
  the plugin. The task also exits non-zero on `INTERNAL_API_USAGES`, which this codebase has had for
  years — read the per-IDE `verification-verdict.txt` under `build/reports/pluginVerifier/` rather
  than trusting the exit code. The `261.*` wildcard that `pluginUntilBuild` carries into that list
  **does** match real `261.x` builds; it looks like it should truncate to `261.0.0` and select
  nothing, but a verifier run reports `PY-261.27258.51`. Check
  `build/reports/pluginVerifier/` before "fixing" it.
- **A platform bump moves more than `platformVersion`.** Four baselines can move with it. Three fail
  *before* your source is even considered, with an error that doesn't name the cause:
  the **Kotlin compiler** must be new enough to read the platform's metadata (a compiler reads
  metadata at most one minor above itself — 2026.2 ships metadata 2.4, so Kotlin 2.2 fails with
  "compiled with an incompatible version of Kotlin"); the **Java toolchain** must match the
  platform's bytecode target (2026.2 emits Java 25, so javac 21 reports "bad class file … wrong
  version 69.0") — and note this is a baseline for **Gradle itself**, not only for the toolchain:
  `instrumentCode` runs inside the Gradle daemon and loads platform classes, so on 2026.2 a daemon
  launched on JDK 21 dies with `UnsupportedClassVersionError: … class file version 69.0`, however
  correctly `-Dorg.gradle.java.installations.paths` points at a 25. Set `JAVA_HOME` to the platform's
  own baseline (21 for 2026.1, 25 for 2026.2) — that is the floor under the window described at the
  top of this file, and on a bump it moves before the pinned Gradle's ceiling does. Gradle also will not
  auto-detect a jenv-managed JDK, so pass the path explicitly; and the **`intelliJPlatform`
  gradle-plugin version** decides whether the Python
  plugin's v2 content modules load *in tests* at all (2.16.0 → 2.18.1 took one port from 3361 failing
  tests, of ~3400, down to 1153). Check all three before debugging your own code.

  The fourth is **the libraries the platform bundles that we also depend on**, which fail at *runtime*
  instead and are correspondingly nastier. `kotlin-stdlib` and `kotlinx-serialization` are both pinned
  to the platform's version in `gradle/libs.versions.toml` (`kotlinPlatform`,
  `kotlinxSerializationPlatform`) and forced onto the runtime classpaths in `build.gradle.kts`;
  re-check both against the new IDE. Read the shipped version out of the platform itself rather than
  guessing — e.g. `unzip -p <ide>/lib/intellij.libraries.kotlinx.serialization.core.jar
  META-INF/MANIFEST.MF | grep Implementation-Version`. The Gradle **test** classpath is flat rather
  than plugin-classloader-scoped, so our copy wins there; when it is older than the platform's, classes
  whose serializers were generated against the newer ABI throw `AbstractMethodError` in
  `PluginGeneratedSerialDescriptor.kt`, which names neither this plugin nor serialization, and (see the
  bullet below) takes hundreds of unrelated scenarios down with it. Issue #587 is the write-up; it cost
  101 failures on the 2026.2 port.
- **A patch release is worth the same two checks, and they are cheap.** A `2026.2.1` → `2026.2.2`
  bump moves `platformVersion` only — the build number stays `262.x`, so `pluginSinceBuild` /
  `pluginUntilBuild` and the manifest do not move — but the bundled libraries above still can.
  Rather than hunting version strings, diff the jars between the two downloaded distributions
  (`shasum -a 256 lib/intellij.libraries.kotlinx.serialization.core.jar` in each): byte-identical
  means nothing moved. That detour is worth taking because `kotlin-stdlib` is not shipped as a jar
  carrying `Implementation-Version` at all — on 2026.2 it is folded into `lib/util-8.jar`, which has
  no manifest, and the version is only readable by decompiling `kotlin.KotlinVersionCurrentValue`.
  Then run the full suite against it: 2026.2.2 was green at the same count with no source change.
- **`./gradlew printProductsReleases` lists what the build asks it to list.** It is configured here
  for the RELEASE and EAP channels; with EAP alone it once reported a 262 build *older* than the one
  being built against, which reads as "you are up to date" and is not. For what is actually
  released, `https://data.services.jetbrains.com/products/releases?code=PY&type=release&latest=false`
  gives version, build number and date.
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
