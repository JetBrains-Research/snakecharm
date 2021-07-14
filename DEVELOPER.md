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
