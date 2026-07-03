# Porting SnakeCharm to PyCharm / IntelliJ Platform 2026.1 (build 261)

**Status: source port complete; the plugin compiles and loads against 2026.1. Test-suite
triage is in progress.** This branch (`update-for-intellij-2026.1`) targets the unified 2026.1
platform. All ~37 source-level API breaks are fixed, `compileKotlin` and `compileTestKotlin`
both succeed, and three test-runtime blockers have been resolved (Kotlin stdlib alignment, the
test-data-path layout, and the community `PyTypeShed` helpers lookup). Non-cucumber test
failures are down from 129 → ~23. Two buckets remain — an obfuscated Pro helpers-locator crash
that blocks the cucumber suite, and ~23 parser golden-file diffs. **If you are picking this up,
jump to [Remaining test-suite fallout — START HERE NEXT TIME](#remaining-test-suite-fallout--start-here-next-time).**

The minimal "just make it load" change (raising `pluginUntilBuild` to `261.*` while still
building against PyCharm Community 2025.2) was tried on #569 and **validated as non-viable** —
see [Why not just raise `pluginUntilBuild`?](#why-not-just-raise-pluginuntilbuild-validated-569).
So this source port is the only path to real 2026.1 support.

## Background: PyCharm was unified

- PyCharm Community and Professional were merged into a single product in 2025.1.
- **2025.2 was the last standalone PyCharm Community release.** From 2025.3 on there is one
  unified PyCharm (free core tier + paid Pro tier; the tier is a runtime license state).
- The 2026.1 IDE is distributed only under the **Professional artifact** (`platformType = PY`,
  build `261.x`). There is no `pycharm-community:2026.1`, so building against 2026.1 requires
  switching `platformType` from `PC` to `PY`.

Because the source changes below bind the Python plugin API in 2026.1-only shapes (e.g. `PyType`
as a Kotlin interface), **the built plugin runs only on 2026.1+**. `pluginSinceBuild` was raised
`252 → 261` and the plugin version set to `2026.1.0` accordingly (versioning scheme:
`YEAR.MAJOR` = minimal compatible platform). Advertising 2025.2 support that the binary cannot
honor would reproduce exactly the "installs then crashes" failure mode #569 was rejected for.

## Why not just raise `pluginUntilBuild`? (validated, #569)

The tempting shortcut is to ship the unchanged 2025.2 binary and just widen
`pluginUntilBuild` to `261.*` so 2026.1 lets it load (PR #569). **This was tested with the
IntelliJ Plugin Verifier and it does not work** — the plugin would install on 2026.1 and
then crash at runtime, which is strictly worse than the current honest "incompatible"
rejection.

Verified against **`PY-261.22158.340`** (PyCharm Professional 2026.1):

```
Plugin SnakeCharm:2025.2.3-eap.SNAPSHOT against PY-261.22158.340: 4 compatibility problems
#Access to unresolved class com.jetbrains.python.validation.ReturnAnnotator
  - SnakemakeVisitorFilter.<init>()                → NoSuchClassError
  - SmkReturnAnnotator.visitPyReturnStatement(...) → NoSuchClassError
  - SmkReturnAnnotator (class)                     → NoSuchClassError
  - SmkReturnAnnotator.<init>()                    → NoSuchClassError
(+ 7 scheduled-for-removal, 4 deprecated incl. PyAnnotator, 155 experimental, 8 internal — not blockers)
```

All 4 hard problems are the removed `ReturnAnnotator` (see item 2 below): a metadata-only
widening cannot satisfy them — they require the source changes on this branch. This is the
concrete proof that #569's approach is a dead end.

## What this branch does (build infrastructure)

- `gradle/wrapper/gradle-wrapper.properties` + `gradleVersion`: **Gradle 8.13 → 9.6.0**.
- `gradle/libs.versions.toml`: **IntelliJ Platform Gradle Plugin 2.7.0 → 2.16.0**; added a
  `kotlinPlatform = "2.3.20"` version (the Kotlin bundled in the target platform — see item 6).
- `gradle.properties`: `platformType = PY`, `platformVersion = 2026.1.3`,
  `pluginSinceBuild = 261`, `pluginUntilBuild = 261.*`, `pluginVersion = 2026.1.0`.
- `build.gradle.kts`: adapted to plugin-2.16.0 / Gradle-9.6 API changes, plus a runtime-only
  `resolutionStrategy` forcing kotlin-stdlib to the platform version (item 6).
- `CHANGELOG.md`: added a `[2026.1.0]` section (the changelog plugin's `changeNotes` lookup
  requires a section matching `pluginVersion`, else `patchPluginXml` fails).

## Source-level API breaks — FIXED

1. **`PyType` is now a Kotlin interface** (verified by decompiling
   `intellij.python.psi.jar!/com/jetbrains/python/psi/types/PyType.class`; `getName()` carries
   `@Nullable`). Implementations changed:
   - `override fun getName(): String` → `override val name: String?`.
   - `override fun isBuiltin(): Boolean` → `override val isBuiltin: Boolean`.
   - `getCompletionVariants(completionPrefix: String?, location, context: ProcessingContext)`:
     `context` is now non-null; return type `Array<out Any>`.
   - Fixed in: `AbstractSmkRuleOrCheckpointType`, `SmkRuleLikeSectionArgsType`,
     `SmkRuleLikeSectionType`, `SmkWildcardsType`, and `SmkSectionNameArgInPySubscriptionLikeReference`
     (`getVariants()` return-type covariance). `PyStructuralType` is still a Java class but its
     `getName`/`isBuiltin` are now seen through the Kotlin `PyType` as properties, so subclasses
     must use `override val` too.

2. **`com.jetbrains.python.validation.ReturnAnnotator` was removed.** The "return outside of
   function" check moved into the `final` `PySyntaxAnnotator`, which batches ~16 internal
   visitors (incl. `PyReturnYieldAnnotatorVisitor`) and is run by `PyCompositeAnnotator`
   **without consulting `PythonVisitorFilter`** (verified in bytecode). So neither the old
   subclass-`ReturnAnnotator` trick nor the `PythonVisitorFilter` suppression works anymore.
   - **New approach:** a `daemon.highlightInfoFilter` — `SmkReturnHighlightInfoFilter` — vetoes
     the `HighlightInfo` for the `ANN.return.outside.of.function` error when the `return` sits
     inside a snakemake `run:` / `onstart` / `onerror` / `onsuccess` block
     (`SmkRunSection` / `SmkWorkflowPythonBlockSection`). `HighlightInfoHolder.add()` consults
     these filters for annotation-produced infos, so this is the correct surgical hook. Top-level
     `return`s in a `.smk` file are still flagged, matching the old behaviour exactly.
   - `SmkReturnAnnotator` deleted; removed from `SmkStandardAnnotatorManager`. The
     `ReturnAnnotator` entry removed from `SnakemakeVisitorFilter` (its 3 inspection entries are
     still gated via `PyFileImpl.isAcceptedFor` and were kept).

3. **`CustomFoldingBuilder.buildLanguageFoldRegions`** now takes `MutableList<FoldingDescriptor?>`
   (nullable element). Fixed in `SmkMakeFoldingBuilder` (+ its private `collectDescriptors`).

4. **`super` disambiguation** in `SmkSLReferenceExpressionImpl.getType` → `super<PyReferenceExpressionImpl>`.

## Test-infrastructure breaks

5. **`com.intellij.testFramework.PlatformLiteFixture` was removed.** `PyLexerTestCase` (base of
   `SnakemakeLexerTest`, `SmkSLLexerTest`) now extends `BasePlatformTestCase`; the full test
   application already registers the Python token-set contributors, so the manual
   `initApplication()` / `registerExtensionPoint(...)` bootstrapping is gone.

6. **Kotlin coroutines "Debug metadata version mismatch. Expected: 1, got 2"** crashed the test
   IDE during project setup. The 2026.1 platform bundles **Kotlin 2.3.20**, but our build's older
   kotlin-stdlib was pulled onto the runtime/test classpath (via `kotlinStdlibJdk8`,
   `kotlin-reflect`, `kotlin-test-junit`) and its coroutine stack-trace recovery cannot read the
   v2 `@DebugMetadata` the platform's classes emit. Fixed with a **runtime-only**
   `resolutionStrategy.force` (build.gradle.kts) pinning `kotlin-stdlib{,-jdk7,-jdk8}` to
   `kotlinPlatform` (2.3.20). Scoped to `*RuntimeClasspath` only — forcing it on the compile
   classpath would trip the compiler's metadata-version check.

7. **Test data path resolution broke** (`SnakemakeTestUtil.getTestDataPath()`). It walked a fixed
   number of parent dirs up from the plugin jar to find the project home. The 2026.1 IntelliJ
   Platform Gradle Plugin sandbox added an extra directory level
   (`.sandbox_pycharm/<projectName>/PY-2026.1.3/...` vs `.sandbox_pycharm/PC-2025.2/...`), so it
   resolved to `.sandbox_pycharm/testData` (nonexistent). Rewritten to walk up to the nearest
   ancestor that actually contains `testData` — layout-independent. This one fix cleared three
   symptoms: the `FileNotFoundException` parsing failures, the `PyLightProjectDescriptor.kt:45`
   `MockPackages3` NPE, and the cucumber `snakemake_api.yaml` `PluginException`
   (`SnakemakeApiYamlAnnotationsService`/`SmkWrapperStorage` derive paths from it).

8. **`PyTypeShed` helpers-root lookup crashed every type-inferring test — PARTIALLY fixed.**
   `PyTypeShed.getDirectory` → `PythonHelpersLocator.getHelpersRoots` iterates the registered
   helpers locators; each does `findRootByJarPath` → `PluginManagerCoreKt.getPluginDistDirByClass`,
   which throws `IllegalStateException: .../python-ce/lib/modules should be lib directory` because
   the Python plugin's v2 content modules live in `lib/modules/*.jar` (the locator expects the jar
   directly under `lib/`). The **community** locator (`PythonHelpersLocator` from `PythonCore`)
   checks the `idea.python.helpers.path` system property first, so we set
   `-Didea.python.helpers.path=<platformPath>/plugins/python-ce/helpers` on the `test` JVM via a
   `jvmArgumentProvider` (`intellijPlatform.platformPath` gives the path). That silenced the
   `python-ce` crash. **See the still-open Pro-locator variant in the next section.**

## Remaining test-suite fallout — START HERE NEXT TIME

Run `./gradlew test -PsnakemakeWrappersRepoPath=testData/wrappers_storage`. After all fixes above,
non-cucumber failures dropped **129 → ~23**; the two open buckets:

1. **Cucumber suite (~all scenarios) — obfuscated Pro helpers locator, same `lib/modules` bug.**
   With the community locator fixed (item 8), the crash now comes from **`PythonProHelpersLocator`**
   (the Pro `Pythonid` plugin, jar `plugins/python/lib/modules/intellij.python.core.impl.jar`):
   `PythonProHelpersLocator.getRoot → PythonHelpersLocator.findRootByJarPath → getPluginDistDirByClass`,
   throwing `.../plugins/python/lib/modules should be lib directory`. This class is **obfuscated**
   (methods `f`/`a`, string constants encoded as longs) and, unlike the community locator, does
   **not** read any `idea.*.helpers.path` property, so the same trick doesn't apply.
   - Established: snakecharm's `defaultExtensionNs="Pythonid"` EPs (`typeProvider`,
     `dialectsTokenSetContributor`, `pyReferenceResolveProvider`, …) are declared by **`PythonCore`**
     via `qualifiedName="Pythonid.…"` (not by the Pro `Pythonid` plugin), and snakecharm only
     `<depends>PythonCore</depends>` — so the Pro plugin is not actually required by snakecharm.
   - **Known upstream issue (no fix yet):**
     <https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070> — "The `lib/modules`
     should be lib directory Exception". Same crash, reported for the 2025.3 platform (same v2-module
     layout), **open with no maintainer fix**. So there is no blessed workaround; the notes below are
     ours.
   - **Root cause (important — this is TEST-ONLY, not a real-user bug):** decompiling
     `PluginManagerCoreKt.getPluginDistDirByClass` shows it returns the plugin path **directly** when
     the class is loaded by a `PluginAwareClassLoader`, and only does the broken "parent dir must be
     named `lib`" walk otherwise. In a real IDE install the Python plugins load via proper plugin
     classloaders, so this never fires. It only fires in the **flattened gradle test sandbox
     classpath**, where the python plugin classes aren't under a `PluginAwareClassLoader`. Do **not**
     do anything user-visible about this (e.g. don't suppress Pro Python at runtime — that would break
     real users who use Pro Python + SnakeCharm).
   - **Most promising fix to try next — declare the Python plugins as bundled deps so they get proper
     classloaders in tests.** In `build.gradle.kts` the `when (platformType)` block currently declares
     only `bundledPlugin("Pythonid")` for `"PY"/"PD"` (and `com.intellij.platform.images`) — it does
     **not** declare `bundledPlugin("PythonCore")`, yet `PythonCore` is where the *community* helpers
     locator lives. Try adding `bundledPlugin("PythonCore")` alongside `Pythonid` for `PY/PD`. If both
     python plugins are declared as bundled dependencies, their classes should load via
     `PluginAwareClassLoader` in the test sandbox and `getPluginDistDirByClass` takes the safe branch —
     fixing **both** locators with no suppression and no `idea.python.helpers.path` hack. (Verify; the
     `idea.python.helpers.path` jvmArg from item 8 may then become unnecessary.)
   - **Fallback (test-only, smelly — last resort):** `-Didea.suppressed.plugins.id=Pythonid` on the
     `test` JVM disables *only* the Pro plugin in tests (via `DisabledPluginsState`, which reads that
     comma-separated id list). Experimentally this **removed the `lib/modules` crash** (0 occurrences
     in the sandbox log). But it changes what we test (no Pro Python) and is a smell — prefer the
     bundled-dep fix above. Note the earlier `-Didea.required.plugins.id=SnakeCharm` *allowlist* is the
     wrong tool: it made things **worse** (25 → 94 failed) by dropping other needed bundled plugins.
   - Also worth a shot: bump the IntelliJ Platform Gradle Plugin `2.16.0 → 2.17.0` (the build nags
     about it) and/or a newer `2026.1.x` platform build, in case #2070 gets fixed upstream.

2. **`SnakemakeParsingTest` / `SmkSLParsingTest` (~23 tests) — golden-file drift.** No longer
   `FileNotFoundException` (that was bucket 7); now `FileComparisonFailedError` — the PSI tree
   printed by the 2026.1 Python parser differs from the committed `psi/*.txt` expectations. Review
   the diffs per file and regenerate the goldens where the differences are benign platform changes.

## Reproducing

```shell
# JDK 21 (jenv picks it up from .java-version in this repo, or set JAVA_HOME manually)
./gradlew compileKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage      # OK
./gradlew compileTestKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage  # OK
./gradlew test -PsnakemakeWrappersRepoPath=testData/wrappers_storage               # two buckets above

# To run one feature only: add `@here` to the .feature and set the runner's
# tags = "not @ignore and @here" in AllCucumberFeaturesTest (remember to revert both).
```
