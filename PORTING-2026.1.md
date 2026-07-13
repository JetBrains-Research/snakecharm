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
| implicit-symbol resolve/completion (`expand`, `temp`, section vars, SmkSL injections) | ~80 | snakecharm's `elementsCache` symbols vanish while the parallel `getSynthetic()` symbols survive (see below) | **Hypothesis** — asymmetry confirmed in the data; mechanism needs one diagnostic run |
| `min_version` inspection + `snakemake_api.yaml` fqn resolution | ~36 | snakemake version / package detection via `PythonPackageManager.forSdk(sdk).listInstalledPackagesSnapshot()` — one of the **most-rewritten 2026.1 APIs** (new packaging/uv model) | **Plausible** — unverified |
| spellchecker + misc | ~10 | separate, not yet triaged | Unknown |

**The implicit-symbol hypothesis is concrete and testable.** In `SmkImplicitPySymbolsResolveProvider`
two lookup paths feed off the same cache:
- `cache.getSynthetic(scope)` returns its `LookupElement`s **raw** → `os`, `sys`, `snakemake`, `Path`,
  `rules`, `config` all still resolve.
- `cache.filter(scope, name)` → `ImplicitPySymbolsCacheImpl.get()` → `validElements()`, which
  **drops any `ImplicitPySymbol` whose `psiDeclaration.isValid` is false** and fires an *async*
  `scheduleUpdate()` that never completes inside the synchronous test window.

So if the library `PyFunction` PSI collected from the mock SDK (by `collectTopLevelMethodsFrom(
"snakemake.io", …)`) gets invalidated — a very plausible consequence of a 2026.1 PSI-lifecycle
change — every `elementsCache` symbol silently disappears, while the synthetic ones are unaffected.
That asymmetry is *exactly* what the failures show (`os` resolves, `expand` returns 0). **If this
holds, one fix in the cache lifecycle clears ~80 failures.** It still has to be distinguished from
the simpler "the cache was never populated" (submodule `resolveQualifiedName("snakemake.io")` or
`.topLevelFunctions` returning empty) — both produce the same 0-result symptom; a single diagnostic
run (log `elementsCache` size + `isValid` in the resolver) tells them apart.

**Why this matters for review.** The honest framing for the PR is: *the crashes are fixed and are
platform-structural; the residual failures are a small number of behavioural root causes, each
likely a single fix, not a pile of golden-file rubber-stamping.* Do not "just regenerate goldens" for
the ~80 resolve failures — that would bake a real regression into the expectations. The ~6 typeshed
goldens *are* legitimate expectation updates.

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

**Next step (fast, do this first):** confirm the implicit-symbol hypothesis with **one `@here`
diagnostic run** of `implicit_py_symbols_resolve.feature` — add a temporary log in
`SmkImplicitPySymbolsResolveProvider.resolveName` printing `cache.get(scope).size` and each
`ImplicitPySymbol.psiDeclaration.isValid`. If the cache has the symbols but they're `isValid == false`,
the fix is in the cache lifecycle (`ImplicitPySymbolsCacheImpl.validElements` drops them + fires an
async `scheduleUpdate` that never runs in the sync test window). If the cache is **empty**, the break
is upstream in `collectTopLevelMethodsFrom` / `resolveQualifiedName("snakemake.io")` /
`.topLevelFunctions` under the mock SDK. Either way it's a *single* fix for ~80 failures, not per-test
triage. Only the ~6 typeshed goldens (`Path`→`pathlib/__init__.pyi`, `sys`) are legitimate expectation
updates.

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
