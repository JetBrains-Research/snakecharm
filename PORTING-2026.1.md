# Porting SnakeCharm to PyCharm / IntelliJ Platform 2026.1 (build 261)

This document is the engineering rationale for the 2026.1 port (PR #570): **what changed and
why**, so the diff can be reviewed as a set of deliberate, traceable responses to platform changes
rather than churn. It targets the unified 2026.1 platform on branch `update-for-intellij-2026.1`.

**Status.** The source port is complete: the plugin compiles and loads against 2026.1, all ~37
source-level API breaks are fixed, `compileKotlin`/`compileTestKotlin` both succeed, and every
test-runtime *crash* blocker is resolved (Kotlin stdlib alignment, the test-data-path layout, and
both `PyTypeShed` helpers-locator crashes). The cucumber suite now **runs** (was 3248/3248
crashing) and the parser golden tests are **green**. The remaining cucumber assertion failures are
**mostly pre-existing on 2025.2, not caused by this port** — see [Test state](#test-state-for-review).

## Background: PyCharm was unified

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

## Why not just raise `pluginUntilBuild`? (validated dead end, #569)

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

## Why the port touches so much — one umbrella cause

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

## What this branch does (build infrastructure)

- `gradle/wrapper/gradle-wrapper.properties` + `gradleVersion`: **Gradle 8.13 → 9.6.0**.
- `gradle/libs.versions.toml`: **IntelliJ Platform Gradle Plugin 2.7.0 → 2.16.0**; added a
  `kotlinPlatform = "2.3.20"` version (the Kotlin bundled in the target platform).
- `gradle.properties`: `platformType = PY`, `platformVersion = 2026.1.3`, `pluginSinceBuild = 261`,
  `pluginUntilBuild = 261.*`, `pluginVersion = 2026.1.0`.
- `build.gradle.kts`: adapted to plugin-2.16.0 / Gradle-9.6 API changes, plus a runtime-only
  `resolutionStrategy` forcing kotlin-stdlib to the platform version (see test break 6).
- `CHANGELOG.md`: added a `[2026.1.0]` section (the changelog plugin's `changeNotes` lookup
  requires a section matching `pluginVersion`, else `patchPluginXml` fails).
- `DEVELOPER.md`: added a JDK-21 command-line build/test quickstart and `platformType`/build-number
  notes for the next platform bump.

## Source-level API breaks — FIXED

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

## Test-infrastructure breaks — FIXED

5. **`com.intellij.testFramework.PlatformLiteFixture` was removed.** `PyLexerTestCase` (base of
   `SnakemakeLexerTest`, `SmkSLLexerTest`) now extends `BasePlatformTestCase`; the full test
   application already registers the Python token-set contributors, so the manual
   `initApplication()` / `registerExtensionPoint(...)` bootstrapping is gone.

6. **Kotlin coroutines "Debug metadata version mismatch. Expected: 1, got 2"** crashed the test IDE
   during project setup. The 2026.1 platform bundles **Kotlin 2.3.20**, but our build's older
   kotlin-stdlib was pulled onto the runtime/test classpath and its coroutine stack-trace recovery
   cannot read the v2 `@DebugMetadata` the platform emits. Fixed with a **runtime-only**
   `resolutionStrategy.force` (build.gradle.kts) pinning `kotlin-stdlib{,-jdk7,-jdk8}` to
   `kotlinPlatform` (2.3.20). Scoped to `*RuntimeClasspath` only — forcing it on the compile
   classpath would trip the compiler's metadata-version check.

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
     a `jvmArgumentProvider`.
   - **Pro** (`PythonProHelpersLocator`, obfuscated, reads no helpers-path property) is fixed by
     **unregistering just that one locator from the `com.jetbrains.python.pythonHelpersLocator` EP
     in the test JVM only** — in `StepDefs.configureSnakemakeProject`, after
     `TestApplicationManager.getInstance()` and before `PythonMockSdk.create`. The EP is
     `dynamic="true"`, so removal is clean; the rest of the Pro Python plugin stays intact, so
     Python resolution still works.

   This is a **test-only** artifact, not a real-user bug: `getPluginDistDirByClass` returns the
   plugin path directly when the class loads via a `PluginAwareClassLoader` (the real IDE case), and
   only does the broken "parent dir must be named `lib`" walk on the flattened gradle test
   classpath. So nothing user-visible is (or should be) changed at runtime.

## Test state (for review)

With the crashes gone, the cucumber suite runs and surfaces ~147 **assertion** failures (previously
invisible — the Pro-locator crash aborted every scenario before any assertion ran). These are
**mostly pre-existing on 2025.2, not caused by the port.**

**Proof (branch-vs-master diff on the largest feature, `Resolve implicitly imported python
names`):**

```
branch (PY/2026.1): 59 failing   master (PC/2025.2): 57 failing
  shared (pre-existing, environmental): 57
  only on 2026.1 (port-introduced):      2  (typeshed stub reorg)
  only on master:                        0  (master ⊂ branch)
```

The **2 port-caused** failures are the bundled **typeshed upgrade** turning single-file stubs into
package stubs (`sys.py` → `sys/__init__.pyi`, `pathlib.pyi` → `pathlib/__init__.pyi`, etc.).
These are legitimate golden updates and are **fixed on this branch** by updating the expectations
in `src/test/resources/features/resolve/implicit_py_symbols_resolve.feature`.

The other **57 shared** failures are a **fresh-checkout test-fixture gap**: bare-`snakemake`
(`MockPackages3`) rows return `resolveQualifiedName("snakemake") = []`, while the versioned
`MockPackages3_smk_<ver>` rows resolve fine — and this fails **identically on 2025.2**. It is
orthogonal to the port. **Please don't rubber-stamp goldens beyond the typeshed ones, and please
don't expand this PR to chase the environmental failures.**

## Related work & open items

- **Upstream gradle-plugin [#2070](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070)** —
  the root cause of the helpers-locator crashes (v2 content-module jars on a flat test classpath).
  If fixed upstream, the EP-unregister workaround (break 8) could be dropped. Worth retrying with a
  newer IntelliJ Platform Gradle Plugin (`2.16 → 2.17`, the build nags) and/or a newer `2026.1.x`.
- **The pre-existing bare-`snakemake`/`MockPackages3` fixture gap** — out of scope here. A separate
  branch off `master` will first reproduce/surface it (in CI or local testing) and then attempt a
  fix; issues will be filed once it's understood.
- **Full-suite master diff (open)** — the 57/59 result above was proven for the largest feature.
  Repeating the diff for the whole suite would enumerate the other buckets (`min_version`,
  `snakemake_api.yaml`, spellchecker, section-name resolution) as pre-existing vs port-caused. This
  is the remaining item before un-drafting the PR.
- Related upstream issues touching the resolve/indexing behaviour behind the environmental gap:
  [#533](https://github.com/JetBrains-Research/snakecharm/issues/533) (rewrite `onChange` to drop
  `SlowOperations`) and [#506](https://github.com/JetBrains-Research/snakecharm/issues/506)
  (dumb-mode crash).

## Reproducing

```shell
# JDK 21 (jenv picks it up from .java-version, or set JAVA_HOME manually)
./gradlew compileKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage      # OK
./gradlew compileTestKotlin -PsnakemakeWrappersRepoPath=testData/wrappers_storage  # OK
./gradlew test -PsnakemakeWrappersRepoPath=testData/wrappers_storage               # runs; remaining assertion failures ~57/59 of the biggest feature proven pre-existing on 2025.2
```
