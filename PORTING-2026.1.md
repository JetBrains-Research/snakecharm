# Porting SnakeCharm to PyCharm / IntelliJ Platform 2026.1 (build 261)

**Status: source port complete; the plugin compiles and loads against 2026.1. Test-suite
triage is in progress.** This branch (`update-for-intellij-2026.1`) targets the unified 2026.1
platform. All ~37 source-level API breaks are fixed, `compileKotlin` and `compileTestKotlin`
both succeed, and the test-runtime *crash* blockers are all resolved: Kotlin stdlib alignment,
the test-data-path layout, and **both** `PyTypeShed` helpers-locator crashes (community *and* the
obfuscated Pro one). The cucumber suite now **runs** (was 3248/3248 crashing) and the parser
golden-file tests (`SnakemakeParsingTest`, `SmkSLParsingTest`) are **green**. What remains is a
single bucket of ~147 *assertion* failures (not crashes) that are behavioural, not
infrastructural — and, importantly, **they are not 147 independent problems.** They collapse to a
handful of systemic causes, all downstream of one event (the PyCharm unification + Python-plugin
v2 rewrite). **If you are picking this up, read
[Why the port touches so much — one umbrella cause](#why-the-port-touches-so-much--one-umbrella-cause-a-few-systemic-effects)
first (it frames the whole PR), then jump to
[Remaining test-suite fallout — START HERE NEXT TIME](#remaining-test-suite-fallout--start-here-next-time).**

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

## Why the port touches so much — one umbrella cause, a few systemic effects

This PR is large, and at first glance the change set (≈37 source breaks, a build overhaul, several
test-infra fixes, and ~147 remaining test failures) looks like unexplained churn. It is not. **Every
change traces back to a single event**: between 2025.1 and 2026.1 JetBrains did not merely bump a
version — they *restructured the product and rewrote the Python plugin*. Three concrete structural
moves happened at once, and each change on this branch is downstream of one of them:

1. **The product was unified** (2025.1 merged Community + Professional; 2025.3 was the *last*
   standalone PyCharm Community — see the Background section). This forced `platformType` `PC → PY`
   and re-shaped the Python plugin API surface: `PyType` became a Kotlin interface, the standalone
   `ReturnAnnotator` was folded into the `final` `PySyntaxAnnotator`, `CustomFoldingBuilder`'s
   signature gained nullability, etc. → **the ~37 source-level breaks in "Source-level API breaks"
   below.** These aren't gratuitous; they're the minimum needed to bind the new API shapes.

2. **The Python plugin was repackaged as v2 content modules** — its code now lives in
   `.../python-ce/lib/modules/*.jar` and `.../python/lib/modules/*.jar` rather than as jars directly
   under `lib/`. → **the `PlatformLiteFixture` removal, the test-data-path extra directory level, and
   both `PyTypeShed` helpers-locator crashes (`lib/modules should be lib directory`, upstream #2070).**
   This one packaging change is responsible for most of the *test-infrastructure* section.

3. **The bundled toolchain was upgraded**: Kotlin `2.3.20` (coroutine `@DebugMetadata` v2, item 6)
   and — critically for the remaining failures — **a newer bundled typeshed**.

### The remaining ~147 failures are ~4 systemic causes, not 147 bugs

Once the crashes were fixed, the cucumber suite ran and exposed ~147 *assertion* failures. They were
previously invisible because the Pro-locator crash aborted every scenario before any assertion ran.
Bucketed by root cause:

| Bucket | ≈count | Root cause | Confidence |
|---|---|---|---|
| stdlib resolve goldens (`Path`→`pathlib.pyi`, `sys`) | ~6 | **typeshed upgrade**: single-file stubs became *package* stubs (`pathlib.pyi` → `pathlib/__init__.pyi`, `sys.py` → `sys/__init__.pyi`) | **Confirmed** — verified on disk in the bundled `python-ce/helpers/typeshed/stdlib` |
| implicit-symbol resolve/completion (`expand`, `temp`, section vars, SmkSL injections) | ~80 | the `SmkImplicitPySymbolsProvider` cache is **empty at resolve time** — its rebuild races the resolve check (see below) | **Confirmed** by instrumented diagnostic run |
| `min_version` inspection + `snakemake_api.yaml` fqn resolution | ~36 | snakemake version / package detection via `PythonPackageManager.forSdk(sdk).listInstalledPackagesSnapshot()` — one of the **most-rewritten 2026.1 APIs** (new packaging/uv model) | **Plausible** — unverified |
| spellchecker + misc | ~10 | separate, not yet triaged | Unknown |

**The implicit-symbol cause is confirmed to be a cache-population race — NOT PSI invalidation.** An
instrumented `@here` run of `implicit_py_symbols_resolve.feature` settled it (the earlier
"`validElements` drops invalidated PSI" theory was **disproven** — invalid count was 0 in every
sample). What the diagnostic showed:

- The resolution *logic* is fine. `SmkImplicitPySymbolsResolveProvider.resolveName` resolves `expand`,
  `rules`, `wildcards`, … **correctly when the cache is populated** (`elementsInScope=46 hit=true`) and
  fails **only** when the cache is empty at that instant (`elementsInScope=0 hit=false`). Same code,
  two outcomes — so it is not a resolver bug and not a goldens problem.
- The break is on the **population side**. `SmkImplicitPySymbolsProvider.doRefreshCache` produced
  `elements=0` in ~121 of ~160 invocations even with a valid `sdk=Mock` and `dumb=false`; only ~39
  produced the full `elements≈50`. Many of the empties are *legitimate* (the scenario deliberately
  switches to the `_wo_snakemake` / none / invalid SDK and asserts non-resolution) — but the failing
  asserts are the cases where a **with-snakemake SDK is active yet the cache is still empty**.
- Root mechanism: cache rebuild is **async and smart-mode-gated**. An SDK/settings change fires an
  event → `doRefresh` → `onChange` → `DumbService.runWhenSmart { runReadAction { doRefreshCache } }`
  (and `scheduleUpdate` always goes through `SwingUtilities.invokeLater`). The cucumber
  `reference should resolve` step gates only on `DumbService.waitForSmartMode()`, which does **not**
  wait for that queued rebuild. In 2026.1, changing the project SDK triggers a re-index (dumb episode),
  so the rebuild is deferred behind smart-mode and the resolve check wins the race against a
  not-yet-repopulated cache. (The `elements=50 io=[io.py]` vs `elements=52 io=[]` split in the logs is
  a red herring — just the two snakemake layouts, `snakemake/io.py` pre-9.0 vs `snakemake/io/__init__.py`
  for 9.0+ — not non-determinism.)

**Fix direction (one fix, ~80 failures — decided: option 1, see the Decision note below):** make the
implicit-symbol cache deterministic relative to the resolve check. Options, cheapest first:
1. In the cucumber `reference should resolve` / `should not resolve` steps, after `waitForSmartMode()`
   also **drain the pending cache rebuild** (e.g. dispatch queued EDT events, or force a synchronous
   `SmkImplicitPySymbolsProvider` refresh) before asserting. Test-only, smallest blast radius.
2. Make `doRefresh`/`scheduleUpdate` run **synchronously in unit-test mode** (there is already an
   `isUnitTestMode` fast-path in `doRefresh` and `refreshAfterSymbolCachesUpdated`; `scheduleUpdate`'s
   `SwingUtilities.invokeLater` and `onChange`'s `runWhenSmart` are the two spots that still defer).
3. Product-side: have the resolver trigger a synchronous rebuild when the cache is empty but a
   snakemake SDK is active. Largest change; only if the race is also user-visible (flicker on SDK
   switch), which it may well be.

**Decision — RESOLVED to a test-only fix (option 1 family), NOT the product-side option 3.** The
initial lean was option 3 (widest), on the theory that the SDK-change→deferred-rebuild race a user
hits when switching interpreters is a real regression. Investigation of upstream history and platform
docs reversed that. Evidence:

- **Transient unresolved refs during an SDK-change reindex are documented as *expected* platform
  behaviour**, not a bug ([JetBrains: Indexing](https://www.jetbrains.com/help/idea/indexing.html),
  [References and Resolve](https://plugins.jetbrains.com/docs/intellij/references-and-resolve.html)).
  The production path already handles it correctly: `onChange` defers the rebuild via
  `DumbService.runWhenSmart` until indexing completes, then `refreshAfterSymbolCachesUpdated` calls
  `DaemonCodeAnalyzer.restart()` to re-highlight. A real user gets correct results once indexing
  finishes — exactly the platform norm.
- **The 2026.1 change that actually broke the tests is named in the platform testing docs**:
  *"Indexing is now run asynchronously in a background thread … use
  `IndexingTestUtil.waitUntilIndexesAreReady()` / `suspendUntilIndexesAreReady()` to wait for fully
  populated indexes."* ([Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html)).
  So the old `DumbService.waitForSmartMode()` the cucumber steps rely on is simply **no longer
  sufficient** on 2026.1 — the fix belongs in the *test* wait, not in production.
- **Upstream author intent points the same way.** Issue
  [#533](https://github.com/JetBrains-Research/snakecharm/issues/533) (OPEN) is the author wanting to
  **rewrite `onChange()` to remove** the `SlowOperations` workaround, and
  [#506](https://github.com/JetBrains-Research/snakecharm/issues/506) was a dumb-mode
  "write thread only" crash in this same area. Option 3 would push heavy PSI resolution back onto the
  resolve path the platform deliberately moved to background, re-introducing exactly those
  threading/slow-op hazards and fighting the author's own cleanup direction. The author already added
  `isUnitTestMode` synchronous fast-paths in `doRefresh` / `refreshAfterSymbolCachesUpdated`
  (commit `c994cd0f`); the missing piece is only that the *test* doesn't wait for the now-async
  index+rebuild.

**So the fix (option 1, refined):** in the cucumber resolve / non-resolve / SDK-change steps, after
`waitForSmartMode()` also wait for indexes to be ready (`IndexingTestUtil.waitUntilIndexesAreReady()`
if present on the test classpath) **and drain the queued cache rebuild** (dispatch pending EDT events,
or force a synchronous `SmkImplicitPySymbolsProvider` refresh) before asserting. Test-only, no
production change, consistent with the author's existing test-mode-sync pattern and with #533's
direction. Leave production `onChange` alone (its async+daemon-restart behaviour is correct and is
what #533 will clean up separately). Options 2 and 3 above are kept only as documented fallbacks.

**Why this matters for review.** The honest framing for the PR is: *the crashes are fixed and are
platform-structural; the residual failures are a small number of behavioural root causes, each a
single fix, not a pile of golden-file rubber-stamping.* Do **not** "just regenerate goldens" for the
~80 resolve failures — the expectations are correct; the cache is simply empty when asserted. Only the
~6 typeshed goldens (`Path`→`pathlib/__init__.pyi`, `sys`) are legitimate expectation updates.

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

8. **`PyTypeShed` helpers-root lookup crashed every type-inferring test — NOW FULLY fixed (two
   locators, two mechanisms).** `PyTypeShed.getDirectory` → `PythonHelpersLocator.getHelpersRoots`
   iterates **every** registered helpers locator (via the `com.jetbrains.python.pythonHelpersLocator`
   EP) with **no exception guard**, so any one throwing locator kills the whole lookup and thus every
   type-inferring test. Each locator does `findRootByJarPath` → `PluginManagerCoreKt
   .getPluginDistDirByClass`, which throws `IllegalStateException: .../lib/modules should be lib
   directory` because the v2 content modules live in `lib/modules/*.jar` (the locator expects the jar
   directly under `lib/`). There are **two** such locators, fixed separately:
   - **Community** (`PythonHelpersLocatorDefault` from `PythonCore`, jar
     `python-ce/lib/modules/intellij.python.community.helpersLocator.jar`) checks the
     `idea.python.helpers.path` system property *first*, so we set
     `-Didea.python.helpers.path=<platformPath>/plugins/python-ce/helpers` on the `test` JVM via a
     `jvmArgumentProvider` (`intellijPlatform.platformPath` gives the path).
   - **Pro** (`PythonProHelpersLocator` from the Pro `Pythonid` plugin, jar
     `python/lib/modules/intellij.python.core.impl.jar`) is **obfuscated** (methods `f`/`a`, string
     constants encoded as long-XOR) and reads **no** helpers-path property, so it can't be pointed at
     a valid root. Fixed by **unregistering just that one locator from the EP in the test JVM only** —
     see "Remaining fallout" bucket 1 for the how and the alternatives considered.

## Remaining test-suite fallout — START HERE NEXT TIME

Run `./gradlew test -PsnakemakeWrappersRepoPath=testData/wrappers_storage`. All **crash** blockers
are fixed; the cucumber suite runs and the parser goldens are green. What's left is one bucket of
behavioural assertion failures — read
[Why the port touches so much](#why-the-port-touches-so-much--one-umbrella-cause-a-few-systemic-effects)
for the systemic-cause breakdown before diving in.

### FIXED this pass — cucumber Pro-helpers-locator crash

The obfuscated **`PythonProHelpersLocator`** crash (`.../python/lib/modules should be lib directory`,
upstream #2070) blocked *every* cucumber scenario (3248/3248). It is now fixed by **unregistering just
that one locator from the `com.jetbrains.python.pythonHelpersLocator` EP in the test JVM**, in
`StepDefs.configureSnakemakeProject` right after `TestApplicationManager.getInstance()` and before
`PythonMockSdk.create` (which triggers `PyTypeShed`'s lazy init). The EP is `dynamic="true"`, so
`ExtensionPoint.unregisterExtensions { className, _ -> className != "…PythonProHelpersLocator" }` is a
clean removal. This leaves the community locator (fed by the `idea.python.helpers.path` jvmArg,
item 8) and the **rest of the Pro Python plugin intact**, so Python resolution still works in tests.
It only touches the test JVM — runtime is unaffected.

Why *this* mechanism, and what was rejected:
- **`getHelpersRoots()` has no exception guard** (verified by decompiling the community locator: it
  iterates the EP list and calls `getRoot()` on each, no try/catch), so one throwing locator kills the
  whole lookup. Removing the single crashing contribution is the minimal surgical fix.
- **This is a TEST-ONLY artifact, not a real-user bug.** `getPluginDistDirByClass` returns the plugin
  path directly when the class is loaded by a `PluginAwareClassLoader`, and only does the broken
  "parent dir must be named `lib`" walk otherwise. In a real IDE install the Python plugins load via
  proper plugin classloaders, so this never fires. It only fires in the **flattened gradle test
  sandbox classpath**. So do **not** do anything user-visible (e.g. don't suppress Pro Python at
  runtime — that would break real Pro-Python + SnakeCharm users).
- **Rejected: `bundledPlugin("PythonCore")`** (the previous "most promising" idea). Tried it — it does
  **nothing**. The doc's assumption was that declaring the python plugins as bundled deps would make
  them load via `PluginAwareClassLoader` in the sandbox. It doesn't: the IntelliJ Platform Gradle
  Plugin puts `bundledPlugin(...)` jars on the **flat test classpath**, so they never get a plugin
  classloader — that is the entire essence of #2070. (Reverted.)
- **Rejected: `-Didea.suppressed.plugins.id=Pythonid`.** This *works* (0 crashes) but disables the
  whole Pro plugin in tests. On the one feature measured it gave an **identical** failure count to the
  EP-unregister approach (59/59), i.e. no behavioural benefit — so the EP-unregister is strictly
  better (keeps Pro Python live, touches only the one broken locator). Kept as a documented fallback
  only. Note the earlier `-Didea.required.plugins.id=SnakeCharm` *allowlist* is the wrong tool: it made
  things **worse** by dropping other needed bundled plugins.

### FIXED this pass — parser goldens are green

`SnakemakeParsingTest` / `SmkSLParsingTest` were previously feared to be ~23 golden-file diffs. After
the test-data-path fix (item 7) they **pass (0 failures)** — the earlier `FileNotFoundException`s were
the only problem; there is no PSI-tree golden drift. Bucket closed.

### OPEN — ~147 behavioural assertion failures (one bucket, ~4 systemic causes)

With the crash gone, the cucumber suite surfaced ~147 assertion failures (`131 AssertionError +
16 ComparisonFailure`; count from the `-Didea.suppressed.plugins.id=Pythonid` full run, equal to the
EP-unregister approach on the sampled feature — the EP-unregister full-run count is being confirmed).
These are **not** golden-file rubber-stamping; see the
[systemic-cause table and hypotheses](#the-remaining-147-failures-are-4-systemic-causes-not-147-bugs).
Top failing features by count:

```
59  Resolve implicitly imported python names
24  Ensures fqn in snakemake_api.yaml corresponds to resolved reference fqn
12  Resolve for section names in rules and checkpoints
12  Inspection: min_version smaller than the one set in settings
 8  Spellchecker for snakemake-exclusive psi elements
 7  Completion in python part of snakemake file
 6  Resolve/Completion for section variables in SmkSL injections (x2)
 …  (implicit-symbol resolution/completion dominates)
```

**Diagnosis DONE (see the systemic-cause section):** an instrumented `@here` run of
`implicit_py_symbols_resolve.feature` confirmed the ~80 implicit-symbol failures are a
**cache-population race**, not resolver logic, not PSI invalidation, not goldens. `expand`/`rules`/…
resolve correctly whenever `SmkImplicitPySymbolsProvider`'s cache is populated and return 0 only when
it is empty at resolve time; the cache rebuild is async + smart-mode-gated and the cucumber
`reference should resolve` step doesn't wait for it, so 2026.1's SDK-change→re-index episode lets the
resolve win the race. **Next step is the fix, not more diagnosis** — decided (see the Decision note in
the systemic-cause section): **option 1**, a test-only wait for the now-async index + queued cache
rebuild after `waitForSmartMode()` (`IndexingTestUtil.waitUntilIndexesAreReady()` + drain the pending
rebuild). Production `onChange` is left alone — its async+daemon-restart behaviour is correct and is
what upstream #533 will clean up separately. One fix should clear ~80 failures. Only the ~6 typeshed goldens
(`Path`→`pathlib/__init__.pyi`, `sys`) are legitimate expectation updates. The `min_version` /
`snakemake_api.yaml` (~36) and spellchecker (~10) buckets are still unverified.

Also worth a shot: bump the IntelliJ Platform Gradle Plugin `2.16.0 → 2.17.0` (the build nags about
it) and/or a newer `2026.1.x` platform build, in case #2070 gets fixed upstream (would let us drop the
EP-unregister workaround entirely).

## Reproducing

```shell
# JDK 21 (jenv picks it up from .java-version in this repo, or set JAVA_HOME manually)
./gradlew compileKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage      # OK
./gradlew compileTestKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage  # OK
./gradlew test -PsnakemakeWrappersRepoPath=testData/wrappers_storage               # runs; ~147 assertion failures (one open bucket)

# To run one feature only: add `@here` to the .feature and set the runner's
# tags = "not @ignore and @here" in AllCucumberFeaturesTest (remember to revert both).
```
