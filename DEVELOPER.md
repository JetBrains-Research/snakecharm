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
* Plugin bundle is located in ` build/distributions/snakecharm-*.zip`

**Command-line build & test (no IDE required):**

The Gradle build uses a **JDK 21 toolchain** (`javaVersion` in `gradle.properties`) and the
Gradle version pinned in `gradle.properties` (`gradleVersion`). Make sure a JDK 21 is
installed and visible to Gradle before building from the command line. For example:

```shell
# macOS (Homebrew): install a JDK 21
brew install openjdk@21

# Point Gradle at it for this build (or manage per-directory with jenv/asdf/SDKMAN):
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

./gradlew clean buildPlugin        # builds build/distributions/snakecharm-*.zip
./gradlew test                     # runs the JUnit + Cucumber test suite
./gradlew verifyPlugin             # runs the IntelliJ Plugin Verifier
./gradlew runIde                   # launches a sandbox IDE with the plugin installed
```

If Gradle can't auto-detect the JDK, pass it explicitly:
`-Dorg.gradle.java.installations.paths=$JAVA_HOME`.

> **Note on the target IDE.** `platformType`/`platformVersion` in `gradle.properties` select
> the IDE the plugin is built and tested against; it is downloaded automatically on first
> build (a multi-hundred-MB to ~1 GB download). Since PyCharm was unified in 2025.1 and the
> standalone Community Edition ended at 2025.2/2025.3, releases from 2026.1 (build `261`) on
> are distributed under the Professional artifact, so `platformType = PY` is required to
> build against them. The free/Pro split is a runtime license state and does not affect the
> downloaded SDK or building the plugin.


**Configure Tests:**
        
1. Configure tests to use `$PROJECT_DIR$/.sandbox_pycharm` as sandbox directory when running tests  from the IDEA context menu. 
   Change template settings for cucumber test:
   1. Open `Run | Edit Configurations... | Edit configuration templates...| Cucumber Java`
   2. Append to `VM optiopns`: 
       ```
      -Didea.config.path=$PROJECT_DIR$/.sandbox_pycharm/config-test -Didea.system.path=$PROJECT_DIR$/.sandbox_pycharm/system-test -Didea.plugins.path=$PROJECT_DIR$/.sandbox_pycharm/plugins-test -Didea.force.use.core.classloader=true
      ```

2. Checkout `snakemake` project sources and configure as test data. Use the version that
   matches `defaultVersion` in `snakemake_api.yaml` (currently **9.9.0**) — otherwise the
   `snakemake_api.yaml` FQN-resolution and completion/resolve feature tests fail. Modern
   snakemake (9.x) uses a `src/` layout, so the symlink must point at `src/snakemake`:
    ```shell
    cd ~
    git clone https://github.com/snakemake/snakemake.git
    git -C ~/snakemake checkout v9.9.0   # match snakemake_api.yaml defaultVersion

    cd <snakecharm>/testData/MockPackages3
    ln -s ~/snakemake/src/snakemake snakemake   # 9.x src/ layout (older releases: ~/snakemake/snakemake)
    ```
   After changing anything under the mock directories, delete the sandbox VFS cache before
   re-running tests (e.g. `rm -rf .sandbox_pycharm`), per the note at the bottom of this file.

Tests are written in [Gherkin](https://cucumber.io/docs/gherkin). You could run tests:
* Using gradle `test` task
* From IDEA context menu via `Cucumber Java` run configuration
  * Before running first test launch `buildTestWrappersBundle` task  

If you get `Unimplemented substep definition` in all `*.feature` files, ensure:
  * Not installed or disabled: `Substeps IntelliJ Plugin` 
  * Plugins installed: `Cucumber Java`, `Gherkin`

**Update to new Platform API:**
* Inspect libs version in `gradle/libs.versions.toml`, especially `intelliJPlatform` and `kotlin` version. Also `javaVersion` and `gradleVersion` in `gradle.properties`
  * See [GitHub:intellij-platform-gradle-plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin) documentation and [GitHub:intellij-platform-plugin-template](https://github.com/JetBrains/intellij-platform-plugin-template) as plugin example
  * `intelliJPlatform` is intellij-platform-gradle-plugin version, not Intellij Platform itself
  * `qodana` update as well
  * 
* Update platform API and this plugin versions in `gradle.properties`, see `pluginVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformVersion`
  * `pluginVersion` version should be also mentioned in changelog `CHANGELOG.md`
  * Build numbers map to IDE versions per
    [build-number-ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html),
    e.g. `2025.2`=`252`, `2025.3`=`253`, `2026.1`=`261`. Set `pluginUntilBuild` to the
    branch of the newest IDE you actually built/tested against (e.g. `261.*`).
  * `platformType`: PyCharm Community (`PC`) ended at 2025.2/2025.3. From 2026.1 (`261`) on,
    the unified PyCharm ships under the Professional artifact, so use `platformType = PY`
    (the build wires `Pythonid` for `PY`/`PD` and `PythonCore` for `PC`).
  * Check available IDE versions with
    `./gradlew printProductsReleases`, or query
    `https://data.services.jetbrains.com/products/releases?code=PY&type=release` (`PY`=PyCharm).
  * **`./gradlew verifyPlugin` and `pluginUntilBuild`.** The `pluginVerification` block in
    `build.gradle.kts` uses `recommended()`, which resolves the set of IDEs spanning
    `pluginSinceBuild`..`pluginUntilBuild`. Once `pluginUntilBuild` is raised past `252`,
    `recommended()` asks for PyCharm **Community** releases above 2025.2 (e.g.
    `pycharm-community:2025.3`) that **do not exist** — Community ended at 2025.2 — so the task
    fails at dependency resolution (`Could not find python:pycharm-community:2025.3`) before any
    verification runs. To verify against 2026.1+, pin explicit Professional IDEs instead, e.g.
    `ide(IntelliJPlatformType.PyCharmProfessional, "2026.1")`, and drop `recommended()`.
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