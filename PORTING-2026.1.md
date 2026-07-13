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
| implicit-symbol resolve/completion (`expand`, `temp`, section vars, SmkSL injections) | ~80 | `SmkImplicitPySymbolsProvider` cache empty because `resolveQualifiedName("snakemake"[.io])` returns nothing for the **bare `snakemake` (MockPackages3) scenarios** — **root cause still OPEN** (see the correction below) | **Two theories DISPROVEN** by implementation (timing-race; missing mock files) |
| `min_version` inspection + `snakemake_api.yaml` fqn resolution | ~36 | snakemake version / package detection via `PythonPackageManager.forSdk(sdk).listInstalledPackagesSnapshot()` — one of the **most-rewritten 2026.1 APIs** (new packaging/uv model) | **Plausible** — unverified |
| spellchecker + misc | ~10 | separate, not yet triaged | Unknown |

> **⚠️ CORRECTION (this whole sub-section's "cache-population race" conclusion was DISPROVEN by
> implementation — read this box first).** Two successive theories for the ~80 implicit-symbol
> failures were each written up as "confirmed" and then falsified when actually fixed. Do not trust the
> race narrative below; it is kept only as a record of what was ruled out. **Current status: root cause
> OPEN.** See "What was tried and disproven" immediately after.

**What the instrumentation established (still valid):**
- The resolver *logic* is fine. `SmkImplicitPySymbolsResolveProvider.resolveName` resolves `expand`,
  `rules`, `wildcards`, … **correctly when the cache is populated** (`elementsInScope=46 hit=true`) and
  returns nothing **only** when the cache is empty (`elementsInScope=0 hit=false`). Not a resolver bug,
  not a goldens problem.
- The cache is empty because `doRefreshCache`'s `resolveQualifiedName("snakemake")` /
  `resolveQualifiedName("snakemake.io")` **returns `[]`** for a stable set of scenarios — even with the
  with-snakemake SDK active (`activeSdk=Mock Python SDK 3.7`), `dumb=false`, indexes ready, and after a
  forced rebuild. Logged directly: `snakemake=[] snakemake.io=[]` for the failing scenarios vs
  `snakemake=[PsiDirectoryImpl] snakemake.io=[PyFileImpl/PsiDirectoryImpl]` for the passing ones.
- **The failing scenarios are exactly the bare `snakemake` (no-version) examples** — e.g. the
  `| snakemake | exp | expand() | expand | __init__.py |` rows. These use the `MockPackages3` module
  root. The **versioned** rows (`snakemake:5x`, `:9.3.0`, …) use `MockPackages3_smk_<ver>` roots and
  **pass**. Correlated via failing testcase names (`Resolve at top-level #11/#14/#16/…`, `Resolve
  inside rule parameters/run section` bare rows, `Resolve implicit python modules/classes`).

**What was tried and DISPROVEN (each with a real test run):**
1. *PSI-invalidation* (`validElements` drops invalid symbols): disproven — `invalid=[]` in every sample.
2. *Async cache-population race* (the "option 1" fix): implemented `IndexingTestUtil
   .waitUntilIndexesAreReady` + `waitForSmartMode` + forced `scheduleUpdate` + drain EDT queue in the
   resolve steps. **Zero effect — still 59/170 in the `@here` feature.** The forced rebuild in smart
   mode with indexes ready *still* logged `elements=0`, so the failure is not a wait/ordering problem.
3. *Missing gitignored `MockPackages3/snakemake` fixture* (`.gitignore:137` ignores it; StepDefs:63
   relies on it): created it as a symlink → `MockPackages3_smk_9.3.0/snakemake`, then as a **real copy**
   under the root. **Both zero effect — still 59** (with `cleanTest` to force re-run; sandbox roots
   under this checkout so `getTestDataPath()` does read this `testData`). So it is *not* simply missing
   snakemake files.

**The open puzzle:** identical snakemake package content resolves under `MockPackages3_smk_9.3.0` but
**not** under `MockPackages3` — same files, different root dir (the latter also contains `peppy`). So
`resolveQualifiedName("snakemake")` failing for the bare-`snakemake` scenarios is about how that
specific root/module is set up or indexed, not about the files being absent. Root cause not yet found.
Note this may not even be a 2026.1 *regression* — it could be a pre-existing environmental gap (these
scenarios may require local setup the author has). **The decisive next experiment is to run a couple of
the failing bare-`snakemake` rows on `master` (2025.2) in a fresh checkout**: if they fail there too,
these ~40 are environmental, not part of the port; if they pass, it is a real 2026.1 regression to
root-cause in the `MockPackages3` root/index setup. (The `snakemake/snakemake-wrappers` external repo
from #572 feeds the *wrapper-metadata* tests, a different feature; it does not supply `snakemake.io`
symbols, so it is not the fix here.)

**Fix direction — REOPENED (the previously-"decided" option 1 was implemented and did NOT work).**
Earlier this section committed to a test-only fix (option 1: `IndexingTestUtil.waitUntilIndexesAreReady`
+ `waitForSmartMode` + drain the queued rebuild) on the theory that the cache was empty due to an
async-rebuild timing race. **That fix produced zero improvement** (see "What was tried and disproven"
above) because the cache is empty for a deeper reason — `resolveQualifiedName("snakemake")` itself
returns `[]` for the bare-`snakemake`/`MockPackages3` scenarios even in smart mode with indexes ready.
No test-side wait can fix a resolution that returns nothing. **The correct next step is to root-cause
why `MockPackages3` resolution differs from `MockPackages3_smk_<ver>`, and first to establish
regression-vs-environmental by running the failing bare-`snakemake` rows on `master`** (see the open
puzzle above). Until that is known, do not pick a "fix option" — the target is not understood.

*Kept for later, only if the root cause turns out to be a genuine async race after all:* the research
below argued a test-only wait would be preferable to a product-side synchronous rebuild. It is recorded
because the reasoning (not the conclusion) stays useful.
- Transient unresolved refs during an SDK-change reindex are documented as *expected* platform
  behaviour ([Indexing](https://www.jetbrains.com/help/idea/indexing.html),
  [References and Resolve](https://plugins.jetbrains.com/docs/intellij/references-and-resolve.html));
  production `onChange` already defers via `runWhenSmart` then `DaemonCodeAnalyzer.restart()`.
- The platform testing docs say indexing is now async and tests should use
  `IndexingTestUtil.waitUntilIndexesAreReady()` ([Testing FAQ](https://plugins.jetbrains.com/docs/intellij/testing-faq.html)).
- Upstream [#533](https://github.com/JetBrains-Research/snakecharm/issues/533) (OPEN) wants to *remove*
  `SlowOperations` complexity from `onChange`; [#506](https://github.com/JetBrains-Research/snakecharm/issues/506)
  was a dumb-mode "write thread only" crash here — both argue against a product-side synchronous rebuild.

**Future goal (separate issue + PR, NOT this one) — is any residual implicit-symbol issue user-visible?**
If, after the `MockPackages3` puzzle is solved, a real user who switches interpreters (or opens a `.smk`
project mid-index) sees `expand`/`temp`/`rules`/… stay red and not self-heal, that is a genuine product
bug to fix in a follow-up PR — not by expanding this already-large port. Prove it by driving a real
(non-test) 2026.1 IDE, not from the test suite.

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

**Root cause still OPEN — two theories disproven by implementation (see the systemic-cause section for
the full trail).** Instrumentation established that the ~80 implicit-symbol failures are the resolver
finding an *empty* `SmkImplicitPySymbolsProvider` cache, and that the cache is empty because
`resolveQualifiedName("snakemake"[.io])` returns `[]` specifically for the **bare-`snakemake`
(`MockPackages3`) scenarios** (versioned `MockPackages3_smk_<ver>` rows pass). The two fixes attempted —
a test-only wait/rebuild (the "cache-population race" theory) and supplying the gitignored
`MockPackages3/snakemake` fixture (symlink, then real copy) — **each had zero effect (still 59/170)**.
So it is neither a wait/ordering problem nor simply-missing files. **Next step is root cause, not a
fix**: figure out why identical snakemake content resolves under `MockPackages3_smk_9.3.0` but not
`MockPackages3`, and run the failing rows on `master` (2025.2) to establish whether this is a 2026.1
regression at all or a pre-existing environmental gap. Only the ~6 typeshed goldens
(`Path`→`pathlib/__init__.pyi`, `sys`) are confirmed legitimate expectation updates. The `min_version` /
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
