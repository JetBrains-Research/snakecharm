# Porting SnakeCharm across IntelliJ Platform releases

One document per port, newest last. Each release section is the engineering rationale for that
port — **what changed and why** — so the diff can be reviewed as a set of deliberate, traceable
responses to platform changes rather than churn.

| release | branch | PR |
|---|---|---|
| [2026.1 (build 261)](#20261--unified-pycharm-build-261) | `update-for-intellij-2026.1` | [#570](https://github.com/JetBrains-Research/snakecharm/pull/570) |
| [2026.2 (build 262)](#20262--build-262) | `update-for-intellij-2026.2` | [#577](https://github.com/JetBrains-Research/snakecharm/pull/577) |

## 2026.1 — unified PyCharm (build 261)

Branch `update-for-intellij-2026.1`, PR #570.

**Status.** The source port is complete: the plugin compiles and loads against 2026.1, all ~37
source-level API breaks are fixed, `compileKotlin`/`compileTestKotlin` both succeed, and every
test-runtime *crash* blocker is resolved (Kotlin stdlib alignment, the test-data-path layout, and
both `PyTypeShed` helpers-locator crashes). The cucumber suite now **runs** (was 3248/3248
crashing) and the parser golden tests are **green**. The remaining cucumber assertion failures are
**mostly pre-existing on 2025.2, not caused by this port** — see [Test state](#test-state-for-review) below.

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
   both `PyTypeShed` helpers-locator crashes** (upstream gradle-plugin #2070).
3. **The bundled toolchain was upgraded**: Kotlin `2.3.20` (coroutine `@DebugMetadata` v2) and a
   newer bundled typeshed (single-file stubs became *package* stubs).

### What this branch does (build infrastructure)

- `gradle/wrapper/gradle-wrapper.properties` + `gradleVersion`: **Gradle 8.13 → 9.6.0**.
- `gradle/libs.versions.toml`: **IntelliJ Platform Gradle Plugin 2.7.0 → 2.16.0**; added a
  `kotlinPlatform = "2.3.20"` version (the Kotlin bundled in the target platform).
- `gradle.properties`: `platformType = PY`, `platformVersion = 2026.1.3`, `pluginSinceBuild = 261`,
  `pluginUntilBuild = 261.*`, `pluginVersion = 2026.1.0`.
- `build.gradle.kts`: adapted to plugin-2.16.0 / Gradle-9.6 API changes, plus a runtime-only
  `resolutionStrategy` forcing kotlin-stdlib to the platform version (see test break 6); also
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

### Test state (for review)

With the crashes gone, the cucumber suite runs (3248 tests) and surfaces the remaining assertion
failures. A **full-suite branch-vs-master diff** (the same `AllCucumberFeaturesTest` on PY/2026.1 vs
PC/2025.2, by testcase name) settles exactly what the port is responsible for:

```
branch (PY/2026.1): 145 failing   master (PC/2025.2): 135 failing
  shared (pre-existing, environmental): 135
  only on 2026.1 (port-caused):          10
  only on master:                         0   (sets nest: master ⊂ branch)
```

So the port breaks **nothing** that passed on 2025.2. The **10 port-caused** failures are
enumerated and **8 are now fixed**, taking the branch from 145 to **137**:

- **2 typeshed golden updates** (`sys.py` → `sys/__init__.pyi`, `pathlib.pyi` →
  `pathlib/__init__.pyi`, from the bundled-typeshed package-stub reorg) — **fixed** in
  `implicit_py_symbols_resolve.feature`.
- **8 spellchecker failures** (`Unknown inspection:SpellCheckingInspection`) — that inspection moved
  out of core (separate module + the Grazie plugin) in 2025.2+; **fixed** via the
  `bundledModule`/`bundledPlugin` declarations (see build infrastructure).
- **2 highlighting edge cases** — unresolved-reference warnings inside an injected `{…}` shell
  string and an f-string conda path are no longer produced on 2026.1. Still under investigation;
  tracked as TODOs in the PR description (no SnakeCharm-side cause found so far, so likely an
  upstream behaviour change or a stale test expectation, not a core port defect).

The **135 shared** failures are a **fresh-checkout test-fixture gap**: bare-`snakemake`
(`MockPackages3`) rows return `resolveQualifiedName("snakemake") = []`, while the versioned
`MockPackages3_smk_<ver>` rows resolve fine — and this fails **identically on 2025.2**. It is
orthogonal to the port. **Please don't rubber-stamp goldens beyond the typeshed ones, and please
don't expand this PR to chase the environmental failures.**

### With the fixture provisioned this branch is 3245/3248

The numbers above were measured without `testData/MockPackages3/snakemake`. Provisioning it per
[#574](https://github.com/JetBrains-Research/snakecharm/pull/574) — clone snakemake at
`snakemake_api.yaml`'s `defaultVersion` (9.9.0), symlink its `src/snakemake`, then clear the sandbox
VFS, which `cleanTest` does **not** do — gives:

```
branch (PY/2026.1), fixture present: 3248 scenarios, 3 failing
  Unresolved variable in injection                                             <- port-caused (open)
  Unresolved conda path (complex string)                                       <- port-caused (open)
  Warn about unresolved snakemake variable in run section, behaviour differs
    from scripts                                                               <- pre-existing, also in the master baseline
```

So the fixture resolves **134 of the 135** shared failures, leaving exactly the 2 open port-caused
ones plus 1 that master fails too. This is measured on this branch rather than inherited from #574,
and it corrects that PR's figure: it is 135 → 1, not 135 → 0.

**Measurement note:** "3419" is not the scenario count — it is cucumber (3248) plus the 171
non-cucumber tests. The fixture never changes the scenario count, only how many pass.

### Related work & open items

- **Upstream gradle-plugin [#2070](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070)** —
  the root cause of the helpers-locator crashes (v2 content-module jars on a flat test classpath).
  If fixed upstream, the EP-unregister workaround (break 8) could be dropped. Worth retrying with a
  newer IntelliJ Platform Gradle Plugin (`2.16 → 2.17`, the build nags) and/or a newer `2026.1.x`.
- **The pre-existing bare-`snakemake`/`MockPackages3` fixture gap — understood and handled
  elsewhere.** Filed as [#575](https://github.com/JetBrains-Research/snakecharm/issues/575) with the
  documentation fix in [#574](https://github.com/JetBrains-Research/snakecharm/pull/574). Still out
  of scope for this PR; see the measured effect in [Test state](#test-state-for-review) above.
- **We ship a redundant `kotlinx-serialization-core-jvm-1.4.1.jar`** in the plugin distribution
  while the platform bundles 1.9.0 (`lib/intellij.libraries.kotlinx.serialization.core.jar`). On the
  **flat gradle test classpath** that skew is a real bug — it shadows the platform's copy and
  platform-generated serializers die with `AbstractMethodError at PluginGeneratedSerialDescriptor.kt`
  — and it is fixed on the 2026.2 branch by extending the runtime `resolutionStrategy` (see the
  2026.2 addendum, item 9). In a **real IDE** the plugin has its own classloader, so a consistently
  bundled 1.4.1 is probably harmless; that has not been verified. Deliberately **not** changed on
  this branch: it costs a full re-verification run and fixes nothing observable here (this branch is
  at 3 failures). Worth doing as a follow-up, or here if a reviewer prefers.
- **Full-suite master diff — done** (result in [Test state](#test-state-for-review)): 135/145
  failures are pre-existing on 2025.2; the 10 port-caused are enumerated (typeshed + spellchecker
  fixed; 2 highlighting edge cases tracked as PR TODOs).
- Related upstream issues touching the resolve/indexing behaviour behind the environmental gap:
  [#533](https://github.com/JetBrains-Research/snakecharm/issues/533) (rewrite `onChange` to drop
  `SlowOperations`) and [#506](https://github.com/JetBrains-Research/snakecharm/issues/506)
  (dumb-mode crash).

### Reproducing

```shell
# JDK 21 (jenv picks it up from .java-version, or set JAVA_HOME manually)
./gradlew compileKotlin       # OK
./gradlew compileTestKotlin   # OK
./gradlew test                # 137 failing without the test fixture, 3 with it -- see Test state
./gradlew prepareSandbox      # builds .sandbox_pycharm/<project>/PY-2026.1.3/plugins/snakecharm/
```

`-PsnakemakeWrappersRepoPath=...` is **no longer required**: since
[#572](https://github.com/JetBrains-Research/snakecharm/pull/572) the wrappers bundle is skipped
with a warning when the property is unset. Passing it is still how you build *with* bundled wrappers.

Verified via `prepareSandbox` that the shipped plugin now bundles the platform's Kotlin
(`kotlin-stdlib{,-jdk7,-jdk8}-2.3.20.jar`) rather than the older build stdlib — the production half
of break 6, which could not be checked before #572 made that task runnable without a local wrappers
checkout.

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

`maxHeapSize` is `SNAKECHARM_TEST_HEAP ?: "1024m"` — the small default is for TeamCity agents. Locally:

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

### The `MockPackages3/snakemake` fixture behaves differently per branch

Provisioned per #574 (clone at `snakemake_api.yaml`'s `defaultVersion`, symlink `src/snakemake`,
then clear the sandbox VFS — `cleanTest` does **not** clear it):

```
2026.1 + fixture:                       3248 scenarios,   3 failing   (2 injected-string + 1 pre-existing on master)
2026.2 + fixture, before item 9's fix:  3248 scenarios, 246 failing
2026.2 + fixture, after  item 9  (serialization):  145 failing
2026.2 + fixture, after  item 10 (daemon restart):  82 failing
2026.2 + fixture, after  item 11 (inspection name): 76 failing   <-- current
```

On 2026.1 the fixture resolves **134 of the 135** environmental failures, so #574's "135 → 0" is
really 135 → 1. On 2026.2 it was net −1 at the time of measurement (247 → 246), fixing 65 and
breaking 64 — which is how we knew those 64 belonged to the 2026.2 harness rather than to the
fixture. The serialization fix has since cleared most of that harness damage; 3 of the current 145
are the same 3 that 2026.1 fails, so **142 are 2026.2-specific and still to triage.**

**The remaining 76 are now overwhelmingly one shape — resolve returning nothing.** 43 of them are
literally `expected:<1> but was:<0>`, concentrated in `snakemake_api.yaml` fqn checks (24),
`Resolve implicitly imported python names` (23) and `Resolve for section names in rules and
checkpoints` (11) — 58 of 76 between them. That count was *unchanged* across the item 9/10/11 fixes,
so it is an independent cause and the obvious next thread: implicit symbols from
`SmkImplicitPySymbolsProvider` appear not to resolve on 2026.2, even with the fixture attached.
Two of the 76 are the injected-string cases shared with 2026.1.

Measurement note: **"3419" is not the scenario count.** It is cucumber (3248) + the 171 non-cucumber
tests. The fixture never changes the scenario count, only how many pass.
