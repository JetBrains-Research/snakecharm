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
  `snakemakeWrappersRepoPath` is unset (the default), the `:buildWrappersBundle` task is
  skipped with a warning and the plugin is built without bundled wrappers — it runs normally,
  but wrapper name completion has nothing to offer. If the property *is* set and does not point
  at a wrappers checkout, the build still fails loudly rather than quietly shipping without them.
  To include them, point it at a local [snakemake-wrappers](https://github.com/snakemake/snakemake-wrappers)
  checkout whose content matches `snakemakeWrappersRepoVersion`:
  `./gradlew buildPlugin -PsnakemakeWrappersRepoPath=/path/to/snakemake-wrappers`.
  As an alternative, you could locally set `snakemakeWrappersRepoPath` to existing wrappers folder in 
  `gradle.properties` file.

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
* Inspect libs version in `gradle/libs.versions.toml`, especially `intelliJPlatform` and `kotlin` version. Also `javaVersion` and `gradleVersion` in `gradle.properties`
  * See [GitHub:intellij-platform-gradle-plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin) documentation and [GitHub:intellij-platform-plugin-template](https://github.com/JetBrains/intellij-platform-plugin-template) as plugin example
  * `intelliJPlatform` is intellij-platform-gradle-plugin version, not Intellij Platform itself
  * `qodana` update as well
  * 
* Update platform API and this plugin versions in `gradle.properties`, see `pluginVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformVersion`
  * `pluginVersion` version should be also mentioned in changelog `CHANGELOG.md`
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