# Porting SnakeCharm to PyCharm / IntelliJ Platform 2026.1 (build 261)

**Status: work in progress.** This branch (`update-for-intellij-2026.1`) modernizes the
build to target the 2026.1 platform, but the plugin **does not yet compile** against it —
the Python plugin API changed substantially. The minimal "just make it load" change
(raising `pluginUntilBuild` to `261.*` while still building against PyCharm Community
2025.2) lives on a separate branch and is the one intended for an upstream PR.

## Background: PyCharm was unified

- PyCharm Community and Professional were merged into a single product in 2025.1.
- **2025.2 was the last standalone PyCharm Community release.** From 2025.3 on there is one
  unified PyCharm (free core tier + paid Pro tier; the tier is a runtime license state).
- The 2026.1 IDE is distributed only under the **Professional artifact** (`platformType = PY`,
  build `261.x`). There is no `pycharm-community:2026.1`, so building against 2026.1 requires
  switching `platformType` from `PC` to `PY`.

## What this branch already does (build infrastructure)

- `gradle/wrapper/gradle-wrapper.properties` + `gradleVersion`: **Gradle 8.13 → 9.6.0**
  (required by the newer IntelliJ Platform Gradle Plugin).
- `gradle/libs.versions.toml`: **IntelliJ Platform Gradle Plugin 2.7.0 → 2.16.0** (2.7.0
  could not resolve the 2026.1 Python plugin's v2 content modules; it also requires Gradle 9+).
- `gradle.properties`: `platformType = PY`, `platformVersion = 2026.1.3`,
  `pluginUntilBuild = 261.*`.
- `build.gradle.kts`: adapted to plugin-2.16.0 / Gradle-9.6 API changes:
  - `create(type, version, useInstaller = ...)` → `create(type, version) { useInstaller = ... }`
    (`useInstaller` is now a `Property<Boolean>` inside a configuration block).
  - Removed the deprecated `val test by getting(Test::class) { ... }` nesting in the `test`
    task (now a hard error under the Gradle 9.6 Kotlin DSL).

With the above, dependency resolution succeeds and the build gets as far as `compileKotlin`.

## What remains: ~37 source-level API breaks

Run `./gradlew compileKotlin` against PY-2026.1.3 to reproduce. The Python plugin was
restructured into **v2 content modules** (e.g. `com.jetbrains.python.psi.*` now lives in
`python-ce/lib/modules/intellij.python.psi.jar`), and several APIs changed shape:

1. **`PyType` is now a Kotlin interface.** Implementations must change:
   - `override fun getName(): String` → `override val name: String?` (now nullable).
   - `override fun isBuiltin(): Boolean` → `override val isBuiltin: Boolean`.
   - `getCompletionVariants(completionPrefix: String?, location, context): Array<out Any>`
     (prefix now nullable, return type covariant).
   - Affects: `AbstractSmkRuleOrCheckpointType`, `SmkRuleLikeSectionArgsType`,
     `SmkRuleLikeSectionType`, `SmkWildcardsType` (+ the abstract subclasses
     `SmkCheckpointType`, `SmkRulesType`) and
     `SmkSectionNameArgInPySubscriptionLikeReference` (return-type covariance).

2. **`com.jetbrains.python.validation.ReturnAnnotator` was removed.** *(design decision needed)*
   The "return outside a function" check moved into the `final` `PySyntaxAnnotator`, which
   dispatches to an internal, non-extensible `PyReturnYieldAnnotatorVisitor`. SnakeCharm used
   to subclass `ReturnAnnotator` (to allow `return` inside snakemake `run:` / python blocks)
   and disable the stock one via `PythonVisitorFilter`. That hook no longer exists. A new
   approach is required to suppress the false positive for run/python blocks.
   - Affects: `SmkReturnAnnotator`, `SnakemakeVisitorFilter`, `SmkAnnotatorManager`.
   - Note: the `PyUnreachableCodeInspection` / `PyUnboundLocalVariableInspection` /
     `PyShadowingBuiltinsInspection` errors in `SnakemakeVisitorFilter` are a cascade from
     the unresolved `ReturnAnnotator` in the same `listOf(...)`; those inspections still exist.

3. **`CustomFoldingBuilder.buildLanguageFoldRegions`** signature now takes
   `MutableList<FoldingDescriptor?>` (nullable element). Affects: `SmkMakeFoldingBuilder`.

4. **`super` disambiguation**: `SmkSLReferenceExpressionImpl` has multiple supertypes
   exposing the same member; the `super` call needs `super<Type>` qualification.

## Reproducing / next steps

```shell
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew compileKotlin            # ~37 errors, grouped as above
```

Suggested order: fix (1), (3), (4) (mechanical), resolve the cascade in (2) by deciding the
new annotator strategy, then run `./gradlew test` and triage feature-test fallout (there are
likely `findUsages`/`highlighting`/`resolve` Cucumber features that exercise these paths).
