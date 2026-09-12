# Porting SnakeCharm across IntelliJ Platform releases

One document per port, newest last. Each release section is the engineering rationale for that
port — **what changed and why** — so the diff can be reviewed as a set of deliberate, traceable
responses to platform changes rather than churn.

| release | branch | PR |
|---|---|---|
| [2026.1 (build 261)](#20261--unified-pycharm-build-261) | `update-for-intellij-2026.1` | [#570](https://github.com/JetBrains-Research/snakecharm/pull/570) |
| [2026.2 (build 262)](#20262--build-262) | `update-for-intellij-2026.2` | [#577](https://github.com/JetBrains-Research/snakecharm/pull/577) |

## Keeping the branches in sync

Each port branch is stacked on the previous one, and stays current by merging **forward only**:
2026.1 → 2026.2, never the other way. The older branch keeps being reviewed and fixed while the
newer one is being built, so this merge happens repeatedly.

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

## 2026.1 — unified PyCharm (build 261)

Branch `update-for-intellij-2026.1`, PR #570.

**Status.** The source port is complete: the plugin compiles and loads against 2026.1, all ~37
source-level API breaks are fixed, `compileKotlin`/`compileTestKotlin` both succeed, and every
test-runtime *crash* blocker is resolved (Kotlin stdlib alignment, the test-data-path layout, and
both `PyTypeShed` helpers-locator crashes). The cucumber suite now **runs** (was 3248/3248
crashing) and the parser golden tests are **green**. The remaining cucumber assertion failures are
**mostly pre-existing on 2025.2, not caused by this port**; the measured branch-vs-master comparison
that establishes this lives in [#570](https://github.com/JetBrains-Research/snakecharm/pull/570),
where it can be kept current as the suite changes.

### Background: PyCharm was unified

- PyCharm Community and Professional were merged into a single product in 2025.1.
- **2025.2 was the last standalone PyCharm Community release.** From 2025.3 on there is one unified
  PyCharm (free core tier + paid Pro tier; the tier is a runtime license state).
- The 2026.1 IDE is distributed only under the **Professional artifact** (`platformType = PY`,
  build `261.x`). There is no `pycharm-community:2026.1`, so building against 2026.1 requires
  switching `platformType` from `PC` to `PY`.

Because the source changes below bind the Python plugin API in 2026.1-only shapes (e.g. `PyType`
as a Kotlin interface), **the built plugin runs only on 2026.1+**. `pluginSinceBuild` was raised
`252 → 261` and the plugin version set to `2026.1.0` (`YEAR.MAJOR` = minimal compatible platform).
Advertising 2025.2 support the binary cannot honour would reproduce the "installs then crashes"
failure mode #569 was rejected for.

### Why not just raise `pluginUntilBuild`? (validated dead end, #569)

The tempting shortcut is to ship the unchanged 2025.2 binary and widen `pluginUntilBuild` to
`261.*` so 2026.1 lets it load (PR #569). **The IntelliJ Plugin Verifier proves this does not
work** — the plugin installs on 2026.1 then crashes at runtime, strictly worse than an honest
"incompatible" rejection. Verified against `PY-261.22158.340` (PyCharm Professional 2026.1):

```
Plugin SnakeCharm:2025.2.3-eap.SNAPSHOT against PY-261.22158.340: 4 compatibility problems
#Access to unresolved class com.jetbrains.python.validation.ReturnAnnotator
  - SnakemakeVisitorFilter.<init>()                → NoSuchClassError
  - SmkReturnAnnotator.visitPyReturnStatement(...) → NoSuchClassError
  - SmkReturnAnnotator (class)                     → NoSuchClassError
  - SmkReturnAnnotator.<init>()                    → NoSuchClassError
```

All 4 hard problems are the removed `ReturnAnnotator` (see source break 2). A metadata-only
widening cannot satisfy them — they require the source changes on this branch.

### Why the port touches so much — one umbrella cause

Between 2025.1 and 2026.1 JetBrains didn't merely bump a version — they **restructured the product
and rewrote the Python plugin**. Every change on this branch is downstream of one of three
structural moves:

1. **The product was unified** (2025.1 merged Community + Professional; 2025.2 was the last
   standalone Community). This forced `platformType` `PC → PY` and re-shaped the Python plugin API
   surface: `PyType` became a Kotlin interface, the standalone `ReturnAnnotator` folded into the
   `final` `PySyntaxAnnotator`, `CustomFoldingBuilder`'s signature gained nullability, etc. → **the
   ~37 source-level breaks below.**
2. **The Python plugin was repackaged as v2 content modules** — its code now lives in
   `.../python-ce/lib/modules/*.jar` and `.../python/lib/modules/*.jar` rather than directly under
   `lib/`. → **the `PlatformLiteFixture` removal, the test-data-path extra directory level, and
   both `PyTypeShed` helpers-locator crashes** (upstream gradle-plugin #2183).
3. **The bundled toolchain was upgraded**: Kotlin `2.3.20` (coroutine `@DebugMetadata` v2) and a
   newer bundled typeshed (single-file stubs became *package* stubs).

### What this branch does (build infrastructure)

- `gradle/wrapper/gradle-wrapper.properties` + `gradleVersion`: **Gradle 8.13 → 9.6.0**.
- `gradle/libs.versions.toml`: **IntelliJ Platform Gradle Plugin 2.7.0 → 2.16.0**; added
  `kotlinPlatform` and `kotlinxSerializationPlatform`, which record what the target platform bundles
  rather than what we would otherwise choose.
- `.java-version`: added, pinning the JDK for jenv/asdf users. It has to move with `javaVersion` on
  every bump, or those users silently keep building on the old JDK.
- `gradle.properties`: `platformType = PY`, `platformVersion = 2026.1.3`, `pluginSinceBuild = 261`,
  `pluginUntilBuild = 261.*`, `pluginVersion = 2026.1.0`.
  - **`PC → PY` costs a compile-time guardrail.** With `PC` the compile classpath was
    `bundledPlugin("PythonCore")`, so a Professional-only Python API used from `src/main` failed the
    build. On `PY` it is `Pythonid`, while `plugin.xml` still declares only
    `<depends>PythonCore</depends>` and we still support IDEA + the community Python plugin. Such a
    call now compiles *and* passes the suite (the Gradle test classpath is flat) and fails only at
    runtime, for those users, with `NoClassDefFoundError`. There is no community 2026.1 artifact to
    build against, so this has to be watched by hand in review.
- `build.gradle.kts`: adapted to plugin-2.16.0 / Gradle-9.6 API changes, plus a runtime-only
  `resolutionStrategy` forcing kotlin-stdlib and kotlinx-serialization to the platform's versions
  (see test break 6, and #587 for the serialization half); also
  declares `bundledModule("intellij.spellchecker")` + `bundledPlugin("tanvd.grazi")` — spellchecker
  was extracted from core into a separate module (and the `SpellCheckingInspection` tool moved to
  the Grazie plugin) in 2025.2+, and we use its API (`spellchecker.bundledDictionaryProvider`).
- `CHANGELOG.md`: added a `[2026.1.0]` section (the changelog plugin's `changeNotes` lookup
  requires a section matching `pluginVersion`, else `patchPluginXml` fails).
- `DEVELOPER.md`: added a JDK-21 command-line build/test quickstart and `platformType`/build-number
  notes for the next platform bump.

### Source-level API breaks — FIXED

1. **`PyType` is now a Kotlin interface** (verified by decompiling
   `intellij.python.psi.jar!/com/jetbrains/python/psi/types/PyType.class`; `getName()` carries
   `@Nullable`). Implementations changed:
   - `override fun getName(): String` → `override val name: String?`.
   - `override fun isBuiltin(): Boolean` → `override val isBuiltin: Boolean`.
   - `getCompletionVariants(...)`: `context` is now non-null; return type `Array<out Any>`.
   - Fixed in `AbstractSmkRuleOrCheckpointType`, `SmkRuleLikeSectionArgsType`,
     `SmkRuleLikeSectionType`, `SmkWildcardsType`, and `SmkSectionNameArgInPySubscriptionLikeReference`
     (`getVariants()` return-type covariance). `PyStructuralType` is still a Java class but its
     `getName`/`isBuiltin` are now seen through the Kotlin `PyType` as properties, so subclasses
     must use `override val` too.

2. **`com.jetbrains.python.validation.ReturnAnnotator` was removed.** The "return outside of
   function" check moved into the `final` `PySyntaxAnnotator`, which batches ~16 internal visitors
   and is run by `PyCompositeAnnotator` **without consulting `PythonVisitorFilter`** (verified in
   bytecode). So neither the old subclass-`ReturnAnnotator` trick nor `PythonVisitorFilter`
   suppression works anymore.
   - **New approach:** a `daemon.highlightInfoFilter` — `SmkReturnHighlightInfoFilter` — vetoes the
     `HighlightInfo` for `ANN.return.outside.of.function` when the `return` sits inside a snakemake
     `run:` / `onstart` / `onerror` / `onsuccess` block (`SmkRunSection` /
     `SmkWorkflowPythonBlockSection`). `HighlightInfoHolder.add()` consults these filters for
     annotation-produced infos, so this is the correct surgical hook. Top-level `return`s in a
     `.smk` file are still flagged, matching the old behaviour exactly.
   - `SmkReturnAnnotator` deleted and removed from `SmkStandardAnnotatorManager`; the
     `ReturnAnnotator` entry removed from `SnakemakeVisitorFilter` (its 3 inspection entries stay
     gated via `PyFileImpl.isAcceptedFor`).

3. **`CustomFoldingBuilder.buildLanguageFoldRegions`** now takes `MutableList<FoldingDescriptor?>`
   (nullable element). Fixed in `SmkMakeFoldingBuilder` (+ its private `collectDescriptors`).

4. **`super` disambiguation** in `SmkSLReferenceExpressionImpl.getType` →
   `super<PyReferenceExpressionImpl>`.

### Test-infrastructure breaks — FIXED

5. **`com.intellij.testFramework.PlatformLiteFixture` was removed.** `PyLexerTestCase` (base of
   `SnakemakeLexerTest`, `SmkSLLexerTest`) now extends `BasePlatformTestCase`; the full test
   application already registers the Python token-set contributors, so the manual
   `initApplication()` / `registerExtensionPoint(...)` bootstrapping is gone.

6. **Kotlin coroutines "Debug metadata version mismatch. Expected: 1, got 2"** crashed the test IDE
   during project setup. The 2026.1 platform bundles **Kotlin 2.3.20**, but our build's older
   kotlin-stdlib was pulled onto the runtime/test classpath and its coroutine stack-trace recovery
   cannot read the v2 `@DebugMetadata` the platform emits. Fixed with a **runtime-only**
   `resolutionStrategy.force` (build.gradle.kts) pinning `kotlin-stdlib{,-jdk7,-jdk8}` to
   `kotlinPlatform` (2.3.20). Scoped to runtime classpaths only (matched case-insensitively, so it
   covers the production `runtimeClasspath` as well as `testRuntimeClasspath` — the shipped plugin
   must not bundle the old stdlib either) — forcing it on the compile classpath would trip the
   compiler's metadata-version check.

7. **Test data path resolution broke** (`SnakemakeTestUtil.getTestDataPath()`). It walked a fixed
   number of parent dirs up from the plugin jar to find the project home; the 2026.1 sandbox added
   an extra directory level (`.sandbox_pycharm/<projectName>/PY-2026.1.3/...` vs
   `.sandbox_pycharm/PC-2025.2/...`), so it resolved to a nonexistent `.sandbox_pycharm/testData`.
   Rewritten to walk up to the nearest ancestor that actually contains `testData` — layout
   independent. This one fix cleared three symptoms: the `FileNotFoundException` parsing failures,
   the `PyLightProjectDescriptor` `MockPackages3` NPE, and the cucumber `snakemake_api.yaml`
   `PluginException`.

8. **`PyTypeShed` helpers-root lookup crashed every type-inferring test — fixed (two locators, two
   mechanisms).** `PyTypeShed.getDirectory` → `PythonHelpersLocator.getHelpersRoots` iterates
   **every** registered helpers locator with **no exception guard**, so one throwing locator kills
   the whole lookup. Each locator's `getPluginDistDirByClass` throws
   `IllegalStateException: .../lib/modules should be lib directory` because the v2 content modules
   live in `lib/modules/*.jar`. Two such locators, fixed separately:
   - **Community** (`PythonHelpersLocatorDefault`) checks `idea.python.helpers.path` first, so we
     set `-Didea.python.helpers.path=<platformPath>/plugins/python-ce/helpers` on the `test` JVM via
     a `jvmArgumentProvider` — but only when that directory actually exists. Only PyCharm
     distributions bundle it; on other platform types (IDEA + the external Python plugin) pointing
     the property at a nonexistent path is worse than leaving it unset, because the locator takes
     the value verbatim and skips the layout check that would otherwise report the problem.
   - **Pro** (`PythonProHelpersLocator`, obfuscated, reads no helpers-path property) is fixed by
     **unregistering just that one locator from the `com.jetbrains.python.pythonHelpersLocator` EP
     in the test JVM only** — at the top of `PythonMockSdk.create`, which is the single point every
     test path funnels through (the cucumber glue calls it directly; `SnakemakeTestCase` reaches it
     via `PyLightProjectDescriptor.getSdk()`). The EP is `dynamic="true"`, so removal is clean; the
     rest of the Pro Python plugin stays intact, so Python resolution still works.

   This is a **test-only** artifact, not a real-user bug: `getPluginDistDirByClass` returns the
   plugin path directly when the class loads via a `PluginAwareClassLoader` (the real IDE case), and
   only does the broken "parent dir must be named `lib`" walk on the flattened gradle test
   classpath. So nothing user-visible is (or should be) changed at runtime.

### Related work & open items

- **Upstream gradle-plugin [#2183](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2183)** —
  the root cause of the helpers-locator crashes (v2 content-module jars on a flat test classpath).
  Still open. The issue we originally tracked, [#2070](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070),
  was closed on 2026-09-11 **as a duplicate of #2183, not as fixed** — a closed upstream link is not
  evidence the workaround can go. If #2183 is fixed, the EP-unregister workaround (break 8) and the
  EP-registration half on 2026.2 could both be dropped; re-check on each IntelliJ Platform Gradle
  Plugin bump.
- **The pre-existing bare-`snakemake`/`MockPackages3` fixture gap** — a missing test fixture rather
  than a port defect, filed as [#575](https://github.com/JetBrains-Research/snakecharm/issues/575)
  with the setup fix in [#574](https://github.com/JetBrains-Research/snakecharm/pull/574) and the
  recipe in `DEVELOPER.md` → Configure Tests.
- **Platform-bundled libraries we also depend on** — `kotlin-stdlib` and `kotlinx-serialization` are
  both pinned to the platform's version and forced onto the runtime classpaths, because on the flat
  Gradle test classpath our copy otherwise shadows the platform's. What that costs when it drifts is
  written up in [#587](https://github.com/JetBrains-Research/snakecharm/issues/587); the checklist
  for re-checking them on a bump is in `DEVELOPER.md` → "Update to new Platform API".
- Related upstream issues touching the resolve/indexing behaviour behind the environmental gap:
  [#533](https://github.com/JetBrains-Research/snakecharm/issues/533) (rewrite `onChange` to drop
  `SlowOperations`) and [#506](https://github.com/JetBrains-Research/snakecharm/issues/506)
  (dumb-mode crash).

## 2026.2 — build 262

Branch `update-for-intellij-2026.2`, PR #577. Stacked on the 2026.1 port above; everything in that
section still applies.

> This section is a working log kept deliberately blunt: it records **avenues tried and rejected**
> as well as fixes, so the same ground isn't covered twice. Expect it to be tidied before merge.

### 9. kotlinx-serialization ABI skew — FIXED

Exactly the same shape as break 6 (kotlin-stdlib), different library, and worth stating as a general
lesson: **anything the platform both bundles *and* generates code against must be pinned to the
platform's version on runtime classpaths, not just kotlin-stdlib.**

- The platform bundles **kotlinx-serialization-core 1.9.0**
  (`Implementation-Version` in `lib/intellij.libraries.kotlinx.serialization.core.jar`).
- Our `kotlinxCbor` dependency pulled **1.4.1** onto the runtime/test classpath, where it won.
- Platform classes carry serializers generated against the 1.9.0 ABI, so they call methods absent
  from 1.4.1 → `java.lang.AbstractMethodError at PluginGeneratedSerialDescriptor.kt:40`, which
  `TestLoggerFactory` promotes to a test failure.

Fixed by extending the existing runtime-only `resolutionStrategy` block to force
`kotlinx-serialization-core` and `-cbor` to a new `kotlinxSerializationPlatform` version
(`libs.versions.toml`), mirroring how `kotlinPlatform` is handled. Verified via
`gradlew dependencies --configuration testRuntimeClasspath`: `1.4.1 -> 1.9.0`.

This is almost certainly the previously-unexplained textmate failure mode recorded in #577
(`textmate.bundles.VSCodeExtension$$serializer` throwing `AbstractMethodError`, 3.2 GB of log
events, no test results written).

**Measured effect — this one fix removed 101 of 246 failures:**

```
2026.2.1 + fixture, before: 3248 scenarios, 246 failing   (90 TestLoggerAssertionError)
2026.2.1 + fixture, after:  3248 scenarios, 145 failing   (0  TestLoggerAssertionError)
  fixed: 102    newly failing: 1
```

Every `TestLoggerAssertionError` is gone; what remains is real assertion mismatches
(140 `AssertionError`, 5 `ComparisonFailure`). Whole families cleared: all 46 rename failures
(`Rename files in workflow sections`, `Rename elements in SnakemakeSL`, `Rename rules/checkpoints`,
`Rename Sections`), 24 `Resolve implicitly imported python names`, 9 conda/notebook file resolution,
8 conda file-name completion.

**Note for 2026.1 (PR #570):** `2026.1.3` bundles the *same* kotlinx-serialization-core 1.9.0, so
this skew exists there too — it is simply latent, because that branch sits at 3 failures and nothing
exercises the affected path hard enough to surface it. Porting the force upstream is defensible on
correctness grounds, but it would perturb #570's measured baseline for no observed gain, so it is
deliberately **not** done there yet.

### 10. `DaemonCodeAnalyzer.restart()` during highlighting — FIXED (145 → 82)

The single largest cause of failures on this branch, and it did not look like one: 67 scenarios
across 7 features failed with

```
java.lang.AssertionError: PSI/document/model changes are not allowed during highlighting
  at FileStatusMap.assertAllowModifications
  at DaemonCodeAnalyzerImpl.restart
  at SmkImplicitPySymbolsProvider.refreshAfterSymbolCachesUpdated
```

`refreshAfterSymbolCachesUpdated` ran its action synchronously in unit-test mode (clear the resolve
cache, then restart the daemon); production has always deferred it via `invokeLater`. 2026.2
tightened the assertion, and because the symbol-cache refresh can be triggered *by* resolution
during a highlighting pass, the restart landed mid-pass.

**Deferring via `invokeLater` does not fix it** — in unit-test mode the platform pumps that queue
synchronously inside the same operation, so the restart still lands mid-pass (measured: 67 → 66).
What works is skipping the restart entirely in test mode. The two halves do different jobs: the
resolve-cache clear is what makes refreshed symbols visible and tests depend on it; the daemon
restart exists to re-highlight open editors after a *background* cache update, which is meaningless
in a test that drives highlighting explicitly and already runs its own warm-up pass via
`instantiateAndRun(..., canChangeDocument = true)`. Production behaviour is unchanged.

Plausibly the same underlying issue as
[#533](https://github.com/JetBrains-Research/snakecharm/issues/533).

### 11. `PyUnusedLocalInspection` was renamed — FIXED (82 → 76)

2026.2 renamed the inspection's `shortName` to `PyUnusedLocalVariableInspection` (keeping
`suppressId="PyUnusedLocal"`), so 5 scenarios failed with `Unknown inspection:...`. Verified as a
real platform change before touching the feature file: 2026.1.3 registers the old name, 2026.2.1 the
new one, both in `intellij.python.psi.impl.jar` and `python-ce.jar`.

### 12. Implicit-symbol cache left holding invalidated PSI — FIXED (76 → 5)

The biggest remaining cluster (58 of 76) was one cause: `SmkImplicitPySymbolsProvider`'s cache is a
list of `ImplicitPySymbol`s that hold **hard references to library PSI**, and `validElements` drops
any whose `psiDeclaration.isValid` is false. Since 2026.2 the platform invalidates library PSI on
every roots change, so the whole cache dies at once — `invalid 54/54` — and every implicit symbol
resolves to nothing.

Nothing rebuilt it in time. The provider refreshed on two settings events, and `stateChanged`
returns early unless the configured SDK **name** changed:

```kotlin
val sdkNameNotChanged = sdkRenamed || (oldState.pythonSdkName == newSettings.sdkName)
if (sdkNameNotChanged && !sdkRemoved) return
```

`validElements` does call `scheduleUpdate()` when it notices dead PSI, but that is
`SwingUtilities.invokeLater` — the rebuild lands *after* the assertion has already failed.

**Fix:** subscribe to `ModuleRootListener.rootsChanged` and rebuild — but only when
`cache.hasDeadPsi()`, i.e. when the roots change actually killed what we cached. Roots changing is
the one event that fires before the resolve, and dead PSI is the precise condition.

The guard is not cosmetic. Rebuilding on *every* roots change also fixed the 58, but broke
`Check different SDK settings`, which walks the interpreter through none/invalid/python-without-
snakemake and asserts `expand` does *not* resolve at each step: the settings listeners clear the
cache, then a roots change fires and `getActiveSdk()` still hands back a usable SDK, so the symbols
come back. An empty or still-valid cache must be left alone.

**How the pattern gave it away.** In `Resolve implicitly imported python names > Resolve at
top-level` (43 rows), exactly rows 2, 17-19, 25-28, 32, 35-36 failed, in every run since 2026.2.0.1.
That set looks arbitrary until you line it up against the *previous* row: **a row fails iff its
snakemake version is the same as the row before it.** A different version changes the SDK name,
fires the settings event, rebuilds the cache; the same version fires nothing and inherits the dead
one. It is not a property of the symbols — `protected` and `ancient` fail while `directory` and
`temp`, defined 20 lines away in the same file, pass.

Chasing "which symbols are broken" was therefore a dead end for an hour. What settled it in one run
was printing three things: what the cache contained at build time (everything), what
`cache[TOP_LEVEL]` returned at resolve time (nothing), and which events fired in between
(`rootsChanged` always, `enabled` only on the passing rows).

**Debug loop.** `AllCucumberFeaturesTest` has a commented-out `tags="not @ignore and @here"`. Rather
than editing two source files per iteration, the `test` task now forwards an env var:

```shell
CUCUMBER_TAGS='@here' SNAKECHARM_TEST_HEAP=8g ./gradlew test --tests "features.AllCucumberFeaturesTest"
```

Tag one scenario or outline `@here` and the 24-minute suite becomes a 60-second one. Cucumber's
`cucumber.filter.tags` property overrides the `@CucumberOptions` annotation, so the runner itself
needs no edit.

### 13. `snakemake with disabled framework` did not disable it — FIXED

Fixing item 12 exposed this, which the staleness had been hiding. `Given a snakemake with disabled
framework project` never disables anything — it only *skips* the `withSnakemakeFacet(...)` call, on
the assumption that a fresh project starts with the framework off:

```kotlin
if (projectType != "snakemake with disabled framework") {
    withSnakemakeFacet("without")
}
```

That assumption died with the descriptor cache (`11fdec6a`, needed on 2026.2 — see the rejected
avenue above): scenarios sharing a descriptor share the fixture's project, and
`SmkSupportProjectSettings` is a project service, so the setting survives into the next scenario. A
"disabled framework" project therefore inherits whatever the previous scenario enabled.

`Check different SDK settings` asserts `expand` does not resolve there. It passed only because the
implicit-symbol cache was full of dead PSI; with item 12 fixed the cache is live, the framework is
in fact enabled, and `expand` resolves. **The scenario was green for the wrong reason.** The step now
pushes a default (disabled) state explicitly.

Worth remembering when the next test starts failing "because of" a fix: a shared-project test suite
can hold assertions that only hold while something else is broken.

### 14. `PyAnnotator` and `ReturnAnnotator` were removed — FIXED

`com.jetbrains.python.validation.PyAnnotator` exists in 2026.1.3 and is gone in 2026.2. It was the
base class every SnakeCharm annotator extended, supplying `annotateElement()`,
`addHighlightingAnnotation()` and the `holder` field — 22 of the 25 compile errors on the first
build. The platform moved its own annotators to a plain `PyElementVisitor` that takes a
`PyAnnotationHolder` at construction (e.g. `PyReturnYieldAnnotatorVisitor`), which is the shape
adopted here: `SmkAnnotatorBase` holds the holder and re-exposes the `addHighlightingAnnotation`
overloads, so the annotator bodies did not change. The remaining 3 errors were
`SmkSLSubscriptionExpression.acceptPyVisitor` taking a non-null `PyAstElementVisitor`.

The `ReturnAnnotator` extension point went with it; its "'return' outside of function" check moved
into the final `PySyntaxAnnotator`. The false positive for `return` inside snakemake `run:` /
`onstart` / `onerror` / `onsuccess` is now suppressed by a `daemon.highlightInfoFilter`
(`SmkReturnHighlightInfoFilter`) rather than by a custom annotator. `return_annotator.feature`
covers both directions — the suppression, and that a `yield` outside a function is still reported.

**The cost that came with it, and that this port paid twice.** Binding the holder at construction
means annotators can no longer be singletons, and `Annotator.annotate()` is called once per PSI
*element*, not once per pass — so the naive port allocates a holder and a visitor set for every
element of every file it is registered against, on every highlighting pass:

- `SmkSLAnnotatingVisitor` is registered against `language="Python"` with no file guard, so it did
  that for every `.py` file in the project, for users who never open a Snakefile. It now checks the
  containing file first (safe: `SmkSLInjector.isValidForInjection` gates injection on
  `isInsideSmkFile`, so SmkSL never appears outside a Snakemake file).
- `SmkAnnotatorManager` had the `file is SmkFile` guard from the start, which bounds the waste to
  Snakefiles but does not remove it. It now caches the visitors on `AnnotationHolder`'s
  `currentAnnotationSession` — the same scope as the holder they capture, one file and one pass.
  The visitors keep no state between elements, so sharing them within a pass is safe.

Rule for anything registered as an `Annotator`: **`annotate()` is a per-element callback.** Whatever
it builds, it builds hundreds of thousands of times. Guard on the file first, then cache per session.

### Method note: cluster failure *messages*, not test names

Grouping the 145 failures by feature made them look like one big resolve problem. Grouping by the
first line of the JUnit XML `<failure message=...>` immediately split them into four unrelated
causes, three of which were fixable the same day. Do this first, from
`build/test-results/test/*.xml`.

**The Gradle console log does not contain failure messages** — only exception class names
(`java.lang.AssertionError at Assert.java:89`). Grepping it for message text silently returns 0 and
looks like success; one intermediate "fix" here was briefly believed to work for exactly that reason.
Always read the XML.

### Running the suite locally

`SNAKECHARM_TEST_HEAP` sets the test JVM's `maxHeapSize`; left unset, the run gets the IDE's own
vmoptions `-Xmx` (2 GB), which is enough for a healthy suite but not for one that leaks projects.
Locally:

```shell
SNAKECHARM_TEST_HEAP=8g ./gradlew test --tests "features.AllCucumberFeaturesTest"   # ~24 min
```

`testData` is **not** a declared input of the `test` task, so editing a `.feature` file does not
invalidate the cache — use `cleanTest test` after test-data edits.

### Avenues tried and REJECTED — do not retry without new evidence

1. **"The descriptor/SDK caching is the cause of the 2026.2 failures; revert it."** — **Wrong, and
   expensively so.** The caching added in `11fdec6a` is **load-bearing**. Reverting it took the
   suite from **246 → 2235 failures**, and the run exhausted the 2 GB test heap
   (`OutOfMemoryError: Java heap space`, 625 MB dump). Without caching each of 3248 scenarios
   builds its own project and mock SDK and nothing is released.

   The subtlety worth keeping: **the caching's stated justification is stale, but the caching is
   still required.** It was introduced to stop `SdkId` "symbolic id already exists" collisions —
   and those now appear **0 times in every current run**. So it is right to be suspicious of the
   comment, wrong to remove the code. Note also that the failing run's dominant exception was the
   serialization `AbstractMethodError` above; that lead came *out of* this rejected experiment,
   which is the only reason it was worth running.

2b. **"Each descriptor's mock SDK needs a unique name."** — Rejected, and worth recording because
   the reasoning looked airtight. `PyLightProjectDescriptor.getSdk()` passes `sdkNameSuffix = ""`, so
   every descriptor produced `Mock Python SDK 3.7`; on 2026.2 SDKs are workspace-model entities keyed
   by symbolic id, so later ones replace earlier ones and scenarios could resolve against another
   descriptor's roots. (Note this also disproves the claim, once in #577's description, that the SDK
   "cannot be given a unique name because `PyLightProjectDescriptor` is final" — it is *our own file*
   under `src/test/kotlin/com/jetbrains/python/fixtures/`, and `PythonMockSdk.create` already takes a
   suffix.) Deriving the suffix from the root names changed **nothing**: 145 → 145, byte-identical
   failure sets. Reverted rather than kept, since a behaviour change with no measurable effect is
   just noise in a diff that needs defending.

2. **"Version-specific scenarios are cross-contaminated by the shared descriptor cache."** —
   Rejected. Only **7** scenarios in the entire suite use `Given a snakemake:<version> project`, far
   too few to explain 64 failures, and the failing `Incorrect using flag methods` scenarios use the
   plain unversioned `Given a snakemake project`.

3. **"Bumping to a newer patch release will fix some of this."** — Rejected as a fix, kept as a
   target, and then **partly overturned**. The first measurement said `2026.2.0.1 → 2026.2.1` fixed
   **0** tests and broke **46**, all refactoring (`Rename elements in SnakemakeSL`,
   `Rename files in workflow sections`, one rename-lambda quick fix). Those 46 were **not** a
   2026.2.1 regression: they were the serialization ABI skew (item 9) surfacing in the rename path,
   and **all of them pass** once core is pinned to the platform's 1.9.0. With the fix applied,
   2026.2.1 (145 failing) is now *better* than the 2026.2.0.1 baseline it replaced (201 failing),
   so the bump is justified on its own merits.

   **Lesson:** attributing a failure set to the thing you just changed is the obvious inference and
   was the wrong one here. The 46 correlated perfectly with the bump and still had a different
   cause. A like-for-like A/B tells you *that* something changed, never *what*.

4. **"The descriptor cache should memoise the SDK too."** — Rejected, with measurements.
   `PyLightProjectDescriptor.getSdk()` is a *function*, so it builds a fresh `Mock Python SDK
   <level>` on every call even though the descriptor itself is cached; caching it with a `by lazy`
   looks like the obvious completion of `11fdec6a` and is wrong. The light fixture disposes the SDK
   along with the project it was attached to, so the next scenario that reuses the instance dies
   with `AlreadyDisposedException: Requesting a package manager for an already disposed SDK Mock
   Python SDK 3.7`. Measured over the `@here` set
   (`resolve/implicit_py_symbols_resolve`, `resolve/wildcards_resolve`, `resolve/section_args_resolve`,
   `highlighting/implicit_symbols_related_warnings`, 342 scenarios): **14 failures with the memo, 0
   without.** `StepDefs.pythonOnlySdks` gets away with caching only because nothing attaches those
   SDKs to a project.

### The `MockPackages3/snakemake` fixture behaves differently per branch

Provisioned per #574 (clone at `snakemake_api.yaml`'s `defaultVersion`, symlink `src/snakemake`,
then clear the sandbox VFS — `cleanTest` does **not** clear it):

```
2026.1 + fixture:                       3248 scenarios,   3 failing   (2 injected-string + 1 pre-existing on master)
2026.2 + fixture, before item 9's fix:  3248 scenarios, 246 failing
2026.2 + fixture, after  item 9  (serialization):  145 failing
2026.2 + fixture, after  item 10 (daemon restart):  82 failing
2026.2 + fixture, after  item 11 (inspection name): 76 failing
2026.2 + fixture, after  item 12 (symbol cache) + 13:  5 failing   <-- current
```

On 2026.1 the fixture resolves **134 of the 135** environmental failures, so #574's "135 → 0" is
really 135 → 1. On 2026.2 it was net −1 at the time of measurement (247 → 246), fixing 65 and
breaking 64 — which is how we knew those 64 belonged to the 2026.2 harness rather than to the
fixture. The serialization fix has since cleared most of that harness damage; 3 of the current 145
are the same 3 that 2026.1 fails, so **142 are 2026.2-specific and still to triage.**

That prediction held: 58 of the 76 were `resolve returning nothing` in three clusters
(`snakemake_api.yaml` fqn checks 24, `Resolve implicitly imported python names` 23, section names
11), and they were all the one cause in item 12 — the implicit-symbol cache holding dead PSI.

**The 5 that remain:**

| failure | shared with 2026.1? |
| --- | --- |
| `Inspection: Unresolved element > Unresolved variable in injection` | yes |
| `Inspection: Unresolved element > Unresolved conda path (complex string)` | yes |
| `Resolve implicitly imported python names > Warn about unresolved snakemake variable in run section` | yes, and on master |
| `Fixes PyArgumentListInspection related false positives > PyArgumentListInspection works in snakemake files` | **no — 2026.2 only, untriaged** |
| `Fixes for PyTypeCheckerInspection related false positives > PyTypeCheckerInspection works in snakemake files` | **no — 2026.2 only, untriaged** |

The first three are the same three 2026.1 fails, so **only two failures are now 2026.2-specific.**
Both are `ExpectedHighlightingData` mismatches where an expected warning is missing, e.g.
`expand(" ", [<warning descr="Expected a mapping, got int">**1</warning>])` highlighted as plain
`**1` — a type-inference difference, not a resolve one. That is the next thread.

Measurement note: **"3419" is not the scenario count.** It is cucumber (3248) + the 171 non-cucumber
tests. The fixture never changes the scenario count, only how many pass.
