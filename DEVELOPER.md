# Configure Project from Sources
    
**Prerequisites:**
  
* To run tests install IDEA plugins: `Cucumber for Java`, `Gherkin`.
* Also, I recommended installing `Cucumber+` plugin to get better cucumber features editing/highlighting experience.
* Restart IDEA

**Configure project from sources:**

1. Checkout the project
2. In IntelliJ IDEA, select `File | New | Project From Existing Sources...`. Choose import from gradle option.

**Build plugin from sources:**
* Run `./gradlew buildPlugin`
* Plugin bundle is located in `build/distributions/snakecharm-*.zip`
* The bundled snakemake-wrappers metadata is optional for a local build: if
  `snakemakeWrappersRepoPath` is unset (the default), `:buildWrappersBundle` does not run and the
  plugin is built without bundled wrappers — it runs normally, but wrapper name completion has
  nothing to offer. If the property *is* set and does not point at a wrappers checkout, the build
  still fails loudly rather than quietly shipping without them.
  To include them, point it at a local [snakemake-wrappers](https://github.com/snakemake/snakemake-wrappers)
  checkout whose content matches `snakemakeWrappersRepoVersion`:
  `./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers`.
  As an alternative, you could locally set `snakemakeWrappersRepoPath` to existing wrappers folder in 
  `gradle.properties` file.

**Command-line build & test (no IDE required):**

The Gradle build uses the JDK toolchain `javaVersion` names in `gradle.properties` — the same number
`.java-version` in the repo root carries — and the Gradle version pinned there (`gradleVersion`).
Make sure that JDK is installed and visible to Gradle before building from the command line.

**If you use jenv, `.java-version` in the repo root already does this** — it selects the
JDK for you as soon as you `cd` here, so you only need that JDK installed. The version it names
tracks the platform and therefore differs per branch (21 for 2026.1, 25 for 2026.2), so re-check
`java -version` after switching branches rather than assuming the shell followed you. **asdf ignores
`.java-version` unless you set `legacy_version_file = yes` in `~/.asdfrc`** — without it asdf reads
only `.tool-versions`, silently leaves your global JDK active, and you land in exactly the cryptic
Gradle failure described below. Everyone else sets `JAVA_HOME` by hand:

```shell
# Read the version this branch needs rather than hardcoding it; .java-version tracks `javaVersion`
JDK=$(cat .java-version)

# macOS (Homebrew): install it
brew install openjdk@$JDK

# Point Gradle at it for this build. Use a path that pins that version exactly -- jenv/asdf/SDKMAN,
# or the install path itself. Do NOT use `/usr/libexec/java_home -v $JDK`: it treats the version as
# a *minimum*, so on a machine without it you get a newer JDK and exit 0 -- and then a failure that
# names neither the JDK nor the version (`Type T not present` from the pinned Gradle if it is too
# new, `UnsupportedClassVersionError` out of instrumentCode if it is too old).
export JAVA_HOME=$(jenv prefix $JDK)    # or e.g. /opt/homebrew/opt/openjdk@$JDK
"$JAVA_HOME/bin/java" -version          # verify it really says $JDK

./gradlew clean buildPlugin        # builds build/distributions/snakecharm-*.zip
./gradlew test                     # runs the JUnit + Cucumber test suite
./gradlew verifyPlugin             # runs the IntelliJ Plugin Verifier
./gradlew runIde                   # launches a sandbox IDE with the plugin installed
```

If Gradle can't auto-detect the JDK, pass it explicitly:
`-Dorg.gradle.java.installations.paths=$JAVA_HOME`.

> **Note on the target IDE.** `platformType`/`platformVersion` in `gradle.properties` select
> the IDE the plugin is built and tested against; it is downloaded automatically on first
> build (a multi-hundred-MB to ~1 GB download). Since PyCharm was unified in 2025.1 and
> [2025.2 was the last release of the standalone Community Edition](https://www.jetbrains.com/pycharm/whatsnew/2025-3/),
> releases from 2025.3 on are distributed only under the Professional (`PY`) artifact, so
> `platformType = PY` is required to build against them. The free/Pro split is a runtime
> license state and does not affect the downloaded SDK or building the plugin.


**Configure Tests:**
        
1. Configure tests to use `$PROJECT_DIR$/.sandbox_pycharm` as sandbox directory when running tests  from the IDEA context menu. 
   Change template settings for cucumber test:
   1. Open `Run | Edit Configurations... | Edit configuration templates...| Cucumber Java`
   2. Append to `VM optiopns`: 
       ```
      -Didea.config.path=$PROJECT_DIR$/.sandbox_pycharm/config-test -Didea.system.path=$PROJECT_DIR$/.sandbox_pycharm/system-test -Didea.plugins.path=$PROJECT_DIR$/.sandbox_pycharm/plugins-test -Didea.force.use.core.classloader=true
      ```

2. Checkout `snakemake` project sources and configure as test data.

   The unversioned `Given a snakemake project` cucumber scenarios resolve the snakemake API against
   `testData/MockPackages3/snakemake` (gitignored, absent on a fresh checkout). Provide it by
   symlinking the snakemake package source. Two details matter:
   * **Version:** it must match `defaultVersion` in `snakemake_api.yaml`, which the FQN tests assert
     against (e.g. `snakemake.ioutils.subpath.subpath`). Read the version from that file rather than
     hardcoding one, so the fixture follows `defaultVersion` when it is bumped.
   * **Layout:** modern snakemake keeps its package under `src/`, so the symlink target is
     `src/snakemake` (older releases had it at the repo root).

    ```shell
    # run from the project root
    VER=$(awk '/^defaultVersion:/{gsub(/[":]/,"",$2); print $2}' snakemake_api.yaml)
    # works whether or not you already cloned snakemake for an earlier version of this recipe
    [ -d ~/snakemake ] || git clone https://github.com/snakemake/snakemake.git ~/snakemake
    # chained: a bad version must not leave the symlink pointing at the wrong revision
    # -fn replaces the broken symlink left by the old recipe
    git -C ~/snakemake fetch --tags && git -C ~/snakemake checkout "v$VER" &&
      ln -sfn ~/snakemake/src/snakemake testData/MockPackages3/snakemake
    ```

   Check the result before running the suite — the `&&` chain means `ln` never runs if the git steps
   fail, and both a leftover broken symlink and a directory that swallowed the link (`ln` into a real
   directory creates `snakemake/snakemake` and exits 0) look like a provisioned fixture:

    ```shell
    ls -l testData/MockPackages3/snakemake/__init__.py
    ```

   If `defaultVersion` changes later, re-point the fixture the same way — the FQN tests will fail
   against a stale checkout.

   **Gotcha — "zero effect":** the test IDE sandbox persists a VFS/index under `.sandbox_pycharm`
   that **`cleanTest` does not clear**. If you add this fixture *after* having already run the tests
   once, the stale VFS won't see the new files and the failures persist unchanged. Always clear it
   after provisioning the fixture:

    ```shell
    # guarded rather than 2>/dev/null: the sandbox does not exist until you have run the tests once,
    # but a removal that genuinely fails must not be silenced -- that lands you right back here
    [ -d .sandbox_pycharm ] && find .sandbox_pycharm -maxdepth 3 -name system-test -exec rm -rf {} +
    ```

   Use `find`, not `rm -rf .sandbox_pycharm/*/system-test`: the sandbox sits at a different depth
   depending on how tests were launched (`.sandbox_pycharm/system-test` for the run configuration in
   step 1, `.sandbox_pycharm/<ide>/system-test` and `.sandbox_pycharm/<project>/<ide>/system-test`
   for the gradle task, varying by platform-plugin version), and a glob that misses simply deletes
   nothing while looking like it worked.

Tests are written in [Gherkin](https://cucumber.io/docs/gherkin). You could run tests:
* Using gradle `test` task
* From IDEA context menu via `Cucumber Java` run configuration
  * Before running first test launch `buildTestWrappersBundle` task  

To run a **single cucumber feature** from the command line, add a `@here` tag above its
`Feature:` line and set `tags = "not @ignore and @here"` in `AllCucumberFeaturesTest.kt`
(revert both afterwards). Note that `testData` is **not** a declared input of the `test`
task, so after editing any feature/test-data file run `./gradlew cleanTest test` — plain
`test` may serve stale cached results.

If you get `Unimplemented substep definition` in all `*.feature` files, ensure:
  * Not installed or disabled: `Substeps IntelliJ Plugin` 
  * Plugins installed: `Cucumber Java`, `Gherkin`

**Reading test results:**
* `./gradlew test` prints a line per failing scenario as it goes, then a `N tests completed, M failed`
  summary. For a run you are watching, that is the report.
* HTML reports are turned off in `build.gradle.kts` (Windows cannot handle some Cucumber scenario
  names), so what a finished run leaves on disk is the JUnit XML under `build/test-results/test/`.
* To see what a change fixed or broke, compare two runs. Capture each log and reduce it to a sorted
  list of scenario names:

  ```shell
  ./gradlew cleanTest test 2>&1 | tee /tmp/after.log
  sed -n '/ > /s/ FAILED$//p' /tmp/after.log | sort -u > /tmp/after.names
  diff /tmp/before.names /tmp/after.names
  ```

  The `/ > /` address keeps only scenario lines, skipping Gradle's own `> Task :test FAILED` (no
  space before its `>`); `-n` with the `p` flag then prints just the lines the substitution changed.
  Check the resulting line count against the `M failed` in the summary. Use `cleanTest test`, not
  plain `test`: `testData` is not a declared input of the `test` task, so an unchanged-looking build
  can report `:test UP-TO-DATE`, print no scenario lines at all, and leave you diffing against an
  empty file that reads as "everything got fixed".

**Update to new Platform API:**

`PORTING.md` records the previous ports release by release — what broke, why, and how it was fixed
— which is usually the fastest way to see what a bump costs before starting one.

* Inspect libs version in `gradle/libs.versions.toml`, especially `intelliJPlatform` and `kotlin` version. Also `javaVersion` and `gradleVersion` in `gradle.properties`, and `.java-version` in the repo root (the jenv pin — asdf honours it only with `legacy_version_file = yes` — which has to move with `javaVersion` or jenv users silently keep building on the old JDK). Those two files are the only place the number lives: the prose in the quickstart above and in `AGENTS.md` → Build & test deliberately names no version, because when it did, both said 21 for the whole of the 2026.2 port while `javaVersion` said 25 — keep it that way
  * See [GitHub:intellij-platform-gradle-plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin) documentation and [GitHub:intellij-platform-plugin-template](https://github.com/JetBrains/intellij-platform-plugin-template) as plugin example
  * `intelliJPlatform` is intellij-platform-gradle-plugin version, not Intellij Platform itself
  * `qodana` update as well
  * `kotlinPlatform` and `kotlinxSerializationPlatform` are **not our versions to choose** — they
    record what the target platform bundles, and `build.gradle.kts` forces them onto the runtime
    classpaths. Re-read both from the new IDE rather than guessing:
    ```shell
    unzip -p <ide>/lib/intellij.libraries.kotlinx.serialization.core.jar META-INF/MANIFEST.MF | grep Implementation-Version
    # kotlin-stdlib ships merged into lib/util-8.jar, not as its own jar, and carries no manifest
    # version -- read the three ints KotlinVersion is constructed from (e.g. 2 / 3 / 20 -> 2.3.20):
    javap -p -c -cp <ide>/lib/util-8.jar kotlin.KotlinVersionCurrentValue | sed -n '/KotlinVersion get/,/areturn/p'
    ```
    Leaving them stale does not fail the build; it fails at *runtime*, in tests, with an error that
    names neither this plugin nor the library — a `@DebugMetadata` version mismatch for the stdlib,
    or `AbstractMethodError` in `PluginGeneratedSerialDescriptor.kt` for serialization. Because the
    Gradle test classpath is flat, our copy shadows the platform's, and one such error becomes
    hundreds of failed scenarios. Issue
    [#587](https://github.com/JetBrains-Research/snakecharm/issues/587) is the worked example.
  * 
* Update platform API and this plugin versions in `gradle.properties`, see `pluginVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformVersion`
  * `pluginVersion` version should be also mentioned in changelog `CHANGELOG.md`
  * Build numbers map to IDE versions per
    [build-number-ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html),
    e.g. `2025.2`=`252`, `2025.3`=`253`, `2026.1`=`261`. Set `pluginUntilBuild` to the
    branch of the newest IDE you actually built/tested against (e.g. `261.*`).
  * `platformType`: 2025.2 was the last release of standalone PyCharm Community (`PC`). From
    2025.3 on only the unified PyCharm ships, under the Professional artifact, so use
    `platformType = PY` (the build wires `Pythonid` for `PY`/`PD` and `PythonCore` for `PC`).
  * Check available IDE versions with
    `./gradlew printProductsReleases`, or query
    `https://data.services.jetbrains.com/products/releases?code=PY&type=release` (`PY`=PyCharm).
* Update `snakemakeWrappersRepoVersion` to up-to-date, need to be updated on TeamCity CI as well.
 
**Release plugin:**
* Fix version in `build.gradle`
* Fix since/until build versions in `build.gradle`
* Fix change notes in `CHANGES` file
* Use 'publishPlugin' task
                        

------

# Useful Resources for IntelliJ Plugin Development:

* Using Kotlin + Gradle
https://kotlinlang.org/docs/reference/using-gradle.html

* Developing IntelliJ Plugins using `gradle-intellij-plugin` plugin documentation:
https://github.com/JetBrains/gradle-intellij-plugin/blob/master/README.md#gradle

* Creating Your First Plugin
https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started.html

* Custom Language Support plugins
https://www.jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support/prerequisites.html

# Snakemake Resources:

Workflows examples: https://github.com/snakemake-workflows/docs

# Parser & Lexer

## Snakemake language
* Language: `SnakemakeLanguageDialect`
* Parsing Subsystem Descriptor: `SmkParserDefinition`
  * Registered in  `plugin.xml`, EP: `com.intellij.lang.parserDefinition`
  * Links language to
    * Lexer `SnakemakeLexer`
      * Token types: `SmkTokenTypes`
    * Parser `SnakemakeParser`
      * AST node types: `SmkElementTypes`
    * AST tree root element type: `SmkFileElementType`
    * PSI tree rot element: `SmkFile`
* Parser: `Snakemake`
  * Uses `PyParser` API => instead of low level `PsiParser.parse(..)` uses HIG level entry point: `SmkParserContext`
    * `getScope()`, `emptyParsingScope() : SmkParsingScope`
      * Custom scope that helps to memorize that parser is parsing python code blocks in: `onstart`/`onsuccess`/`onerror`/`run` sections
        This knowledge changes parser behaviour for some language constructions
    * `getFunctionParser(): SmkFunctionParsing`
      * **API ignored by SnakeCharm**:
        * customizes python functions parsing
      * **API used**:
        * customisation of PyReferenceExpression class (use SmkPyReferenceExpression class) via `getReferenceType()`.
         
          Required for adding snakemake specific variant into Python expressions code completion & resolve
    * `getExpressionParser(): SmkExpressionParsing`
      * **API ignored by SnakeCharm**:
        * customizes different python expressions parsing (string, star literals, etc)
        
    * `getStatementParser() : SmkStatementParsing`
      * Does main job, **Entry Point** : `parseStatement()`
        * Snakemake keywords 'rule' not python keywords, so they could be freely using in pure python blocs, e.g.
            python methods, `run` section, etc
        * If parser is not in `pure python` block, it changes lexer token for snakemake specific keywords, from `PyTokenTypes.IDENTIFIER` 
            to custom snakemake token types
        
            P.S: SnakemakeLexer also changes the way how lexem generated & count rules sections stack, so parsing is actually started in Lexer
        * If first statement lexeme isn't snakemake specific => delegate parsing of the statement to python parser
        * Else:
          * parse cases (`rule`,`checkpoint`, etc.)
        * Parsing done via:
          * Start new AST node:
            * `marker = myBuilder.mark()`
          * Finish (create new NODE and link to all lexemes between start & finish)
            See `com.intellij.lang.SyntaxTreeBuilder.Marker`
            * `marker.done(NODE_ELEMENT_TYPE)`
            * `marker.error('msg')` - mark whole node as parsing error
              * Better behaviour:
                * `builder.error(msg)` - insert error
                * `marker.done(NODE_ELEMENT_TYPE)` - close current marker with proper element type
            * `maker.drop()` - new block not needed
            * `new_marker = maker.precedes()` - for making hierarchical structures, e.g. `foo.boo.doo.roo`
            * `rollBack(..)` - for lang constructions with similar syntax, when only in the end we could say how to parse the beginning
          * Useful 
            * `builder.advanceLexer()` & `nextToken()`, `atToken()`, `checkMatches()`, `builder.eof()`
  * Test:
    * Lexer: `SnakemakeLexerTest`
    * Parser: `SnakemakeParsingTest`, testdata: `./testData/psi`

## SnakemakeSL language  
* Another Example: `SmkSLParserDefinition`
  * Lexer - generated using JFlex, see `./src/main/kotlin/com/jetbrains/snakecharm/stringLanguage/lang/parser/smk_sl.flex` 
  * Tests
    * Lexer: `SmkSLLexerTest`
      * Token types: `SmkSLTokenTypes`
    * Parser: `SmkSLParsingTest`, testdata: `testData/stringLanguagePsi`
      * AST node types: `SmkSLElementTypes`

## Testdata

### Custom snakemake version

* Create mock directory for custom snakemake version, e.g. for 8.20.6: `./testData/MockPackages3_smk_8.20.6/snakemake`
* Copy only required files (e.g. with canged API) into mock directory
* Use in Cucumber steps, e.g. `Given a snakemake:8.20.6 project`

NB: To run tests locally it is important to delete VFS cache for test instance on any change in mock directories, e.g. `.sandbox_pycharm/PC-2025.1/system-test`