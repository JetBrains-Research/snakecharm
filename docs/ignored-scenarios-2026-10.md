# `@ignore`d Cucumber scenarios: investigation (October 2026)

Why the scenarios tagged `@ignore` were failing, what was fixed and what remains ignored.
Platform `263-EAP-SNAPSHOT`, branch `features/snakemake-9.27-api-support` at `517e6281`.

## How the ignored scenarios were run

`AllCucumberFeaturesTest` filters with `not @ignore`, and the `CUCUMBER_TAGS` passthrough re-applies
that filter (`not @ignore and (...)`, see [Testing](testing.md#focused-cucumber-runs)), so it cannot
select ignored scenarios. Instead, a temporary runner class was added next to `AllCucumberFeaturesTest`
and deleted afterwards:

```kotlin
package features

@RunWith(Cucumber::class)
@CucumberOptions(plugin = ["pretty", "summary"], tags = "@ignore")
class IgnoredTmpCucumberFeaturesTest
```

```shell
./gradlew cleanTest test --tests "features.IgnoredTmpCucumberFeaturesTest"
```

Leave `tags` out and set `features = [...]` instead to run whole feature files, including their
non-ignored scenarios, as a regression check. The run takes about 2 minutes.

`editor content will be` fails with `FileComparisonFailedError: TEXT`, and the JUnit XML does not include
the expected or actual text. To see them, temporarily print `text` and
`fixture().editor.document.text` in `ActionsSteps.editorContentWillBe`. Calling
`FileComparisonFailedError.expectedStringPresentation` does not compile against the test classpath.

## Result

119 ignored scenarios ran and 93 failed. They fall into these groups:

| Group | Feature(s) | Failed | Outcome |
|---|---|---|---|
| `log` in `onstart`/`onerror`/`onsuccess` (#549) | `implicit_py_symbols_resolve`, `implicit_py_symbols_completion` | 9 | fixed |
| `PyShadowingBuiltinsInspection` disabled (#133) | `py_shadowing_builtins` | 1 | fixed |
| Enter after a section at end of file (#558) | `indent_on_new_line_typing` | 1 | fixed |
| Statement mover and `run:` section | `line_mover` | 9 | fixed |
| `pep.config` feature not implemented | `after_pep_config_resolve`, `after_pep_config_completion`, `weak_warning_for_pep_without_pepfile` | 73 | still ignored |
| Empty `TODO` placeholders | `implicit_py_symbols_resolve` (2 scenarios) | 0 | still ignored, nothing to run |
| All scenarios commented out | `controlflow/snakemake_controlflow` | — | still ignored, nothing to run |

## Fixed

### 1. `log` in workflow handlers (#549)

**Symptom:** `print(log)` inside `onstart:`/`onerror:`/`onsuccess:` reported
"Unresolved reference 'log'", and `log` was not offered in completion.

**Cause:** Snakemake compiles these blocks into `def __onstart(log): ...` (`DecoratorKeywordState`
with `args = ["log"]` in `snakemake/parser.py`), but SnakeCharm did not know `log` there. The 2024 fix
(`5e842e50`) was reverted in `f1e984ff`, which left only TODOs.

**Fix:**
- `SmkImplicitPySymbolsResolveProvider`: an unqualified `log` inside `SmkWorkflowPythonBlockSection`
  resolves to a null target. This is the same approach as `jobid` in `run:`: it is a runtime value with
  no declaration to navigate to.
- `SmkImplicitPySymbolsCompletionContributor`: offers `log` there, with the new bundle key
  `TYPES.workflow.handler.variable.type.text`.
- Removed the stale TODOs in `SmkCodeInsightScope` and `SmkImplicitPySymbolsProvider`.
- Two non-ignored scenarios asserted the old wrong behaviour ("Not-completed in top level python
  block" and "Not-resolved in top level python block"). Their `log` rows were removed.

### 2. `PyShadowingBuiltinsInspection` in snakemake files (#133)

**Symptom:** `input = 1` in a `.smk` file was not reported.

**Cause:** this was intentional. The #133 workaround (`c74d94ef`) listed the inspection in
`SnakemakeVisitorFilter`, which switched it off for whole snakemake files so that
`lambda wildcards, input: ...` in rule sections would not be reported. The platform inspection still
has no extension hook: its visitor reports the name identifier directly, with no
`PyInspectionExtension` call.

**Fix:**
- Removed the inspection from `SnakemakeVisitorFilter`.
- `SmkInspectionsSuppressor` suppresses `PyShadowingBuiltins` only for a lambda parameter in a
  `SmkRuleOrCheckpointArgsSection` when snakemake passes that parameter by name. The allowed names come
  from `SnakemakeApiService.getLambdaArgsForSubsection(section, ruleLike)`, e.g. `input` in `params:`.
- New scenario: `lambda wildcards, list:` is still reported.

### 3. Indent after Enter at end of file (#558)

**Symptom:** typing `\noutput: ...` after `input: "foo/{sample}"`, when that line is the last one in the
file, indented the new line by 8 spaces instead of 4. The existing mid-file scenario only passed because
of its trailing `# formatter hack` comment.

**Cause:** section arguments are parsed as a `PyArgumentList` without parentheses. The platform's
`PyBlock.isIncomplete()` treats a `PyArgumentList` with no closing paren as incomplete whenever nothing
follows it. So at end of file the formatter placed the caret inside the arguments and used their indent.

**Fix:** `SmkBlock`, a `PyBlock` subclass in `SnakemakeFormattingModelBuilder.kt`, is now the root
block. Its `buildSubBlock` re-wraps the platform's sub-blocks, so the whole tree uses it. For
arguments of an `SmkArgsSection` it reports "incomplete" only after a trailing comma, an error element,
or when there are no arguments yet. Other blocks fall back to `PyBlock.isIncomplete()`.

### 4. Statement mover and `run:` section

**Symptom** (scenario "Move section out run section doesn't work if statement is bound statement"):
- **Move Down** on the last statement of `run:` moved it into the rule body, where only sections are
  allowed, and re-indented the rule.
- **Move Up** on the first statement corrupted the document:

  ```
  rule NAME:            rule NAME:
    run:          ->      file1="file.txt"
      file1=...
      file2=...
  ```

  The `run:` line and `file2` were lost.

**Cause:**
- Down: `isAvailableForMoveOut` allowed any boundary statement that is not an `SmkArgsSection` to leave
  its statement list.
- Up: `getDestinationElement` returned the enclosing `SmkRunSection` itself. The
  `elementToMove !is SmkSection && destination is SmkSection` branch then built a `ScopeRange` that
  overlapped the moved line.

**Fix** in `SmkStatementMover`:
- No move when the destination is a `SmkRunSection` that contains the element (Up).
- `isAvailableForMoveOut` returns false for boundary statements whose list belongs to a
  `SmkRunSection` (Down).
- A section other than `run` that is next to a comment line now swaps with that line, even when it is
  the first or last section. Swapping with a comment line cannot take a section out of its rule. This
  fixes "Move section up in rule works for execution sections - merge with prev", which was titled
  "IGNORED: …" before. `run:` sections stay immovable, as "Move section up in rule doesn't work if
  statement is run section" expects.

`CHANGELOG.md` has entries for all four fixes under 2026.3.2.

## Still ignored

### `pep.config` (73 scenarios)

The feature is not implemented. It was disabled in `9743246b` (2021, "Need to re-implement caching and
revise whole impl."):
- `SmkPepConfigType`, `SmkPepConfigReference`, `SmkPepReferenceContributor`, `SmkPepConfigCollector`
  and `YamlKeysCollector` are fully commented out.
- The reference contributor is commented out in `plugin.xml`.
- The `SmkPepConfigType` branch in `SmkTypeProvider` is commented out.

These scenarios need the feature to be re-implemented; that is a separate task. A few of them pass, but
only because nothing resolves: for example, the "Not resolve …" scenarios with `.key` access.

**Related bug, not fixed:** `SmkTypeProvider` types `pep` as `PyClass.getType(...)`. That is the type
of the `peppy.Project` *class* (a definition type), not an instance. As a result:
- `pep.config` evaluates to the `property` object.
- Completion after `pep.config.` lists `fget`, `fset`, `deleter`, ...
- With `PyUnresolvedReferencesInspection` enabled, `pep.config.custom_key3` reports
  "Unresolved attribute reference 'custom_key3' for class 'property'".

The likely fix is `(… as? PyClassType)?.toInstance()`. Check it against the passing
`after_pep_object_{resolve,completion}.feature` before changing it.

### Placeholders

- `implicit_py_symbols_resolve.feature`: "Do not resolve at top-level if no python sdk" and "Resolve at
  top-level if custom python sdk" have no steps (`# TODO`). They pass trivially.
- `controlflow/snakemake_controlflow.feature` (issue #14): every scenario is commented out, so the
  feature runs nothing.

## Verification

- With the fixes, the full suite `./gradlew cleanTest test` passed: **3515 tests in 126 suites, 0
  failed**, taking 46m 54s with a cold daemon. This was on `517e6281` plus these changes, with no
  `CUCUMBER_TAGS` set.
- `SnakemakeFormatterTest` and the touched features passed in focused runs.
- In one focused run, `Resolve implicitly imported python names > Check different SDK settings` failed
  once. It passed in two reruns and in the full suite. It is sensitive to SDK and implicit-symbol cache
  timing in a shared project (see [2026.2 porting notes](porting/2026.2.md)), and the failure was not
  caused by these changes.
