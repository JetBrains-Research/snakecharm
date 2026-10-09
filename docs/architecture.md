# Architecture

SnakeCharm extends the bundled Python plugin's lexer, parser, PSI, and code-insight APIs.
Most extension points are registered against `language="Python"`, so implementations must
check whether the file or element belongs to SnakeCharm before doing language-specific work.

Source paths below are relative to `src/main/kotlin/com/jetbrains/snakecharm/`.
Start at [plugin.xml](../src/main/resources/META-INF/plugin.xml) to find feature entry points.
For test setup and execution, see [Testing](testing.md).

## Languages and source map

**Snakemake** (`SnakemakeLanguageDialect`) handles `Snakefile`, `*.smk`, and `*.rule(s)`.
Its parser delegates ordinary Python syntax to the Python parser while recognizing Snakemake
sections outside pure-Python blocks.

**SmkSL** is the string language injected into Python literals such as
`"results/sample_{genome}.bam"`. Its implementation lives under `stringLanguage/`.

| Area | Sources and responsibilities |
|---|---|
| Parsing | `lang/parser/`: lexer, parsing context, section/statement parsing |
| PSI | `lang/psi/`: files, rules, sections; `types/`, `references/`, and `stubs/` |
| Highlighting | `lang/highlighter/`, `lang/validation/`; SmkSL counterparts under `stringLanguage/` |
| Completion and resolve | `codeInsight/`: config, rules, section arguments, wildcards, implicit API, wrappers |
| Inspections | `inspections/`: local inspections registered in `plugin.xml` |
| Framework detection | `framework/`: locating Snakemake through the project SDK/package manager |
| IDE integrations | `lang/structureView/`, `lang/documentation/`, `lang/formatter/`, `spellchecker/`, `actions/` |

Feature tests live under `src/test/resources/features/`; parser and lexer tests also have
dedicated JUnit classes and golden data.

## Snakemake parser and lexer

`SmkParserDefinition` connects `SnakemakeLanguageDialect` to `SnakemakeLexer`,
`SnakemakeParser`, the dialect's file element type, and the root PSI class `SmkFile`.
Tokens are defined by `SmkTokenTypes`; AST element types by `SmkElementTypes`.

`SnakemakeParser` extends `PyParser` and uses its higher-level parsing API through
`SmkParserContext`, rather than implementing a standalone low-level parser.
The context exposes these collaborators:

- `SmkParsingScope` tracks whether parsing is inside Python code, including
  `run:`, `onstart`, `onsuccess`, and `onerror`. Snakemake words such as `rule`
  remain ordinary identifiers inside pure-Python blocks.
- `SmkStatementParsing.parseStatement()` is the main Snakemake statement entry point.
  Outside pure-Python blocks, it remaps identifiers for Snakemake keywords to custom token
  types and parses the corresponding constructs. Other statements go to the Python parser.
- `SmkExpressionParsing` handles rule-like section argument lists and their recovery,
  and selects SnakeCharm's reference-expression element type.
- `SmkFunctionParsing` customizes the reference-expression element type while retaining
  the platform's function parsing. SnakeCharm reference PSI enables its completion and resolve.

The lexer also tracks rule-section nesting and changes token production. When investigating a
parse result, examine both the lexer and the parsing context.

### AST construction and recovery

Parsing uses `com.intellij.lang.SyntaxTreeBuilder.Marker`:

| Operation | Purpose |
|---|---|
| `myBuilder.mark()` | Start an AST node |
| `marker.done(elementType)` | Finish the node around consumed tokens |
| `marker.error(message)` | Mark the entire node as an error |
| `myBuilder.error(message)`, then `marker.done(elementType)` | Report an error while retaining the intended node type |
| `marker.drop()` | Remove a marker without creating a node |
| `marker.precede()` | Start an enclosing node, useful for chained expressions |
| `marker.rollbackTo()` | Rewind speculative parsing |

Token operations include `advanceLexer()`, `nextToken()`, `atToken()`, `checkMatches()`,
and `eof()`. Preserve PSI structure during error recovery where possible: other features
operate on incomplete files while the user types.

`SnakemakeLexerTest` covers tokenization. `SnakemakeParsingTest` uses golden data under
`testData/psi/`.

## SmkSL parser and injection

`SmkSLParserDefinition` connects the string-language parser and lexer.
The lexer is generated with JFlex from
`stringLanguage/lang/parser/smk_sl.flex`; tokens and AST elements are described by
`SmkSLTokenTypes` and `SmkSLElementTypes`.

`SmkSLInjector` controls injection into Python strings inside Snakemake files.
`SmkSLLexerTest` covers tokenization; `SmkSLParsingTest` uses
`testData/stringLanguagePsi/`.

## Framework detection and implicit API

Most features depend on locating the Snakemake package through the project SDK. The plugin
statically analyzes that package where possible. `SmkImplicitPySymbolsProvider` resolves
runtime-provided names such as `expand`, `temp`, `config`, and `rules` by qualified name.

Because Snakemake's API is dynamic and varies by release, the plugin also ships
[snakemake_api.yaml](../snakemake_api.yaml), loaded by `SnakemakeApiYamlAnnotationsService`
into the project-level `SnakemakeApiService`. Snakemake versions act as language levels;
`defaultVersion` selects the default for new projects and the latest officially supported level.
Read its value from the file instead of copying a current version into documentation.

Installed plugins keep the YAML under `extra/snakemake_api.yaml`; users can customize it there.
For example, on macOS the path is
`~/Library/Application Support/JetBrains/PyCharm<version>/plugins/snakecharm/extra/snakemake_api.yaml`.
The tests read the project copy; the default fixture must match its version
([fixture setup](testing.md#snakemake-fixture)).

## Highlighting and annotator lifecycle

The current annotator entry points include `SmkDumbAwareAnnotator`,
`SmkWildcardsAnnotator`, and `SmkSLWildcardsAnnotator`. They create visitors holding a
`PyAnnotationHolder`; these visitors cannot be shared globally as singletons.

`Annotator.annotate()` is called per PSI element. Guard on the containing file before allocating
visitors or doing work, particularly for extensions registered against Python. Respect dumb mode
when a visitor needs indices.

The current implementation constructs visitors per call. Earlier port work experimented with
caching them per `AnnotationHolder.currentAnnotationSession`; that is historical context, not
a description of the current code. If changing allocation or caching, consider holder lifetime,
visitor state, and measured performance. See the
[2026.2 annotator migration](porting/2026.2.md#annotator-migration).

Snakemake Python blocks permit `return` because Snakemake compiles them into generated functions.
`SmkReturnHighlightInfoFilter` suppresses the platform's corresponding false positive for those
blocks; it must preserve diagnostics outside them.

## Platform API boundaries

Building against unified PyCharm exposes Professional-only APIs on the compile classpath.
SnakeCharm still declares `PythonCore` compatibility and supports IntelliJ IDEA with the
community Python plugin, so successful compilation and flat-classpath tests do not prove that
a Professional-only dependency is available to all users. Check API ownership when introducing
platform calls, and use the [porting checklist](porting/README.md) for compatibility work.
