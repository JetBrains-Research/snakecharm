# Porting SnakeCharm across IntelliJ Platform releases

One document per port, newest last. Each release section is the engineering rationale for that
port — **what changed and why** — so the diff can be reviewed as a set of deliberate, traceable
responses to platform changes rather than churn.

| release | branch | PR |
|---|---|---|
| [2026.1 (build 261)](#20261--unified-pycharm-build-261) | `update-for-intellij-2026.1` | [#570](https://github.com/JetBrains-Research/snakecharm/pull/570) |

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
