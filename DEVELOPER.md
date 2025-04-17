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


**Configure Tests:**
        
1. Configure tests to use `$PROJECT_DIR$/.sandbox_pycharm` as sandbox directory when running tests  from the IDEA context menu. 
   Change template settings for cucumber test:
   1. Open `Run | Edit Configurations... | Edit configuration templates...| Cucumber Java`
   2. Append to `VM optiopns`: 
       ```
      -Didea.config.path=$PROJECT_DIR$/.sandbox_pycharm/config-test -Didea.system.path=$PROJECT_DIR$/.sandbox_pycharm/system-test -Didea.plugins.path=$PROJECT_DIR$/.sandbox_pycharm/plugins-test -Didea.force.use.core.classloader=true
      ```

2. Checkout `snakemake` project sources and configure as test data:
    ```shell
    cd ~
    git clone https://github.com/snakemake/snakemake.git

    cd ./testData/MockPackages3
    ln -s ~/snakemake/snakemake snakemake
    ```

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
* Update platform API version in `gradle.properties`, see `pluginVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformVersion`
  * `pluginVersion` version should be also mentioned in changelog `CHANGELOG.md`
 
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