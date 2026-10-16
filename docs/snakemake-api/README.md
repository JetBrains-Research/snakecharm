# Updating to a new Snakemake release

How to bring SnakeCharm up to date with new Snakemake releases: new directives, changed function
signatures, and refactorings that break static analysis. Per-update reports live next to this file and
are named after the new `defaultVersion` (e.g. [9.27.0.md](9.27.0.md)); read the latest one as a worked
example. Background: [Architecture](../architecture.md#framework-detection-and-implicit-api),
[Testing](../testing.md).

## How SnakeCharm models the Snakemake API

Two mechanisms, and an update usually touches both:

1. **`snakemake_api.yaml`**: versioned declarations of directives (sections) and functions:
   which sections exist in which context, which accept keyword/multiple/callable arguments, lambda
   parameters, SmkSL injection, wildcard handling, and `limit_to_sections` for flag functions.
   Snakemake versions act as language levels; `defaultVersion` is the default for new projects. Changelog
   entries are keyed into `TreeMap`s, so their order in the file doesn't matter. `override` must repeat **all**
   non-default fields of the directive; it is not merged with the previous record.
2. **Static analysis of the installed snakemake package**: `SmkImplicitPySymbolsProvider` resolves runtime
   globals (`expand`, `temp`, `rules`, `config`, `input` in `run:`, ...) by **hardcoded module and class
   FQNs**. Related FQNs live in `SnakemakeApi` (`SECTION_ACCESSOR_CLASSES`, `GLOBAL_VARS_TO_CLASS_FQN`) and
   `SnakemakeNames`. A snakemake refactoring that moves a class or function silently breaks resolve,
   completion, types and inspections, without a single error.

Hardcoded in Kotlin rather than yaml: top-level keywords (`SnakemakeApi.TOPLEVEL_ARGS_SECTION_KEYWORDS`,
needed by the lexer), keyword constants in `SnakemakeNames`, and file-reference sections.

## Procedure

### 1. Get the sources

```shell
[ -d ~/snakemake ] || git clone https://github.com/snakemake/snakemake.git ~/snakemake
git -C ~/snakemake fetch --tags origin
OLD=$(awk '/^defaultVersion:/{gsub(/[":]/,"",$2); print $2}' snakemake_api.yaml)
NEW=9.27.0   # target release
for v in "$OLD" "$NEW"; do git -C ~/snakemake worktree add -f ~/smk-$v "v$v"; done
git -C ~/snakemake worktree add -f ~/smk-main origin/main
```

Worktrees let you diff versions side by side and switch the test fixture without re-checkouts.

### 2. Read the changelog

`CHANGELOG.md` in the snakemake repo is the
[history page](https://snakemake.readthedocs.io/en/stable/project_info/history.html):

```shell
awk "/^## \[${OLD//./\\.}\]/{exit} {print}" ~/smk-main/CHANGELOG.md | grep -E "^## |^\* " | grep -viE "^\* (docs|chore)"
```

Mark candidates: new directives and keywords, new helper functions, new callable support,
new globals, changed signatures. `feat!` entries and "refactor/decouple/move" fixes often hide
moved modules. For unreleased `main`, read `git log v$NEW..origin/main`.

Many entries are runtime-only (scheduler, storage, logging, reports) and need nothing.

### 3. Diff the API surface, not only the changelog

The changelog does not announce module moves. Check these directly (replace `$v`):

```shell
d=~/smk-$v/src/snakemake
# directives: keyword classes and the subautomata dicts
grep -nE "def keyword|^class .*KeywordState|subautomata|= dict\(" $d/parser.py
git -C ~/snakemake diff v$OLD v$NEW -- src/snakemake/parser.py
# Snakefile globals
grep -nE '_globals\["|self\.globals\["|register_in_globals' $d/workflow.py
cat $d/ioutils/__init__.py; grep -n register_in_globals -A10 $d/ioflags.py
# package layout changes (files added/removed/renamed)
git -C ~/snakemake diff --stat -M v$OLD v$NEW -- src/snakemake | sort -t'|' -k2 -rn | head -60
```

Then **verify every hardcoded FQN** against old, new and `main`:

```shell
grep -rnoE '"snakemake\.[A-Za-z_.]+"' src/main/kotlin | sort -u
```

For each class or function, `grep -rn "^class X\|^def x" ~/smk-$v/src/snakemake`. A re-export through
`__init__.py` is not enough when it is conditional (`if TYPE_CHECKING:`, `if sys.version_info >= ...:`):
static analysis does not follow it.

To find the version that introduced a change:

```shell
for t in $(git -C ~/snakemake tag --sort=v:refname | grep -E '^v9\.'); do
  git -C ~/snakemake show $t:src/snakemake/<file>.py 2>/dev/null | grep -q '<pattern>' && { echo $t; break; }
done
```

### 4. Implement

**New directive**: add a yaml `introduced` record per context (`top-level`, `rule-like` covers
rule/checkpoint/use, `module`, `subworkflow`) with the right flags and `docs_url`. Check snakemake's
`parser.py` state class and the `Workflow` method it compiles to: `**items` means keyword-only;
callable support appears as `apply_input_function`/`callable(...)` in `rules.py`. For a **top-level**
directive also add a `SnakemakeNames.WORKFLOW_*_KEYWORD` constant and put it in
`TOPLEVEL_ARGS_SECTION_KEYWORDS`.

**Changed directive behaviour** (e.g. now accepts callables): add an `override` record in the new version,
copying all flags of the latest record and changing what's new. `lambda_args` must start with `wildcards`.

**New function**: if it is registered via `ioutils.register_in_globals` or lives in `snakemake.io`/
`snakemake.ioflags`, the provider usually collects it already; confirm it resolves. Add a yaml `function`
record (FQN = defining module + name) for docs, `limit_to_sections`, or `placeholders_injection_allowed`.

**New global** in `workflow.py`: add to `SnakemakeApi.GLOBAL_VARS_TO_CLASS_FQN` (with a `SnakemakeNames`
constant) or a dedicated collector in `SmkImplicitPySymbolsProvider`.

**Moved API**: never replace the old FQN. Add the new location as a fallback (candidate lists,
module lists such as `SNAKEMAKE_MODULE_NAMES_IO_CONTAINERS`), legacy first, so all language levels
keep working. Check every consumer of the old FQN: the provider, `SmkTypeProvider`,
inspections comparing `qualifiedName` or the containing module name (e.g. `SmkMinVersionWarningInspection`).
Also check functions users import explicitly: a re-export makes `from snakemake.utils import min_version`
resolve to the new module.

**Deprecated or removed**: add `deprecated`/`removed` records with an `advice`.

Finally bump `defaultVersion` to the newest release. Do not set it to an unreleased `main` version.

### 5. Tests

- **Fixture**: re-point `testData/MockPackages3/snakemake` to the new `defaultVersion` and clear the
  sandbox VFS, as described in [Testing](../testing.md#snakemake-fixture).
- **Unversioned rows** (`Given a snakemake project`) follow `defaultVersion`. Update their expected files
  and classes to the new layout.
- **Old layout**: keep or add a `Given a snakemake:<version> project` row backed by
  `testData/MockPackages3_smk_<version>/` (copy only the needed files; a versioned mock replaces the
  default fixture, it is not layered). Add a mock only when a layout changes, not for every release.
- **Unreleased `main`**: name the mock after the expected release (e.g. `MockPackages3_smk_10.0.0`),
  copy the real files from `main`, and rename it if the release differs.
- **`defaultVersion` vs `main`**: CI also runs the suite with `main` as the default fixture (see below).
  When `main` moves an API after `defaultVersion`, pin the rows asserting the old file to a
  `snakemake:<defaultVersion>` mock (e.g. `MockPackages3_smk_9.27.0`) and add matching `main` rows. Unversioned
  rows then assert only what both share.
- **Python 3.7 test language level**: `StepDefs` creates test projects with `LanguageLevel.PYTHON37`, so
  imports guarded by `if sys.version_info >= (3, 11):` don't resolve in tests even though they do for users.
  Import from the defining module in such scenarios.
- **yaml FQNs**: add a scenario to `ensure_smk_api_function_fqn.feature` for each new yaml function.
- **Language levels**: test new directives with `I set snakemake language version to "<older>"` (expect the
  "added in version" error) and at the default level (expect no errors). If a directive changes behaviour,
  move rows of "latest level" scenarios into a versioned scenario instead of deleting them.
- Resolve steps that count targets (`times`) catch duplicate symbols from overlapping collectors.

Run focused features with `CUCUMBER_TAGS='@here'` while iterating, then `./gradlew cleanTest test`.

### 6. CI against `main`

The `snakemake_master` TeamCity configuration mounts snakemake `main` as the default fixture. Its
failures fall into three groups:

1. **Unversioned rows asserting release-specific files** (`expected:<__init__.py> but was:<misc.py>`):
   `main` moved something after `defaultVersion`. Pin these rows to a `snakemake:<defaultVersion>` mock and
   cover `main` with a versioned mock (see [Tests](#5-tests)), so the configuration stays green.
2. **Real breakages** (nothing resolves, many unrelated scenarios fail): a moved module or class. Fix
   the FQNs with fallbacks.
3. **Platform noise**: see [Troubleshooting](../testing.md#troubleshooting); cluster failure messages first.

### 7. Wrap up

- Add a `CHANGELOG.md` entry to the unreleased plugin version section ([release checklist](../../DEVELOPER.md#release-checklist)).
- Write `docs/snakemake-api/<defaultVersion>.md`: what changed upstream, what was added, what was not,
  where, and which tests cover it. Use [9.27.0.md](9.27.0.md) as the template.

## Checklist

- [ ] Changelog read from old `defaultVersion` to target; `main` commits checked
- [ ] `parser.py` diff: new or changed keyword states and subautomata
- [ ] `workflow.py` globals and `register_in_globals` diff
- [ ] Every hardcoded `"snakemake.*"` FQN verified in old, new and `main`
- [ ] yaml records added (`introduced`/`override`/`deprecated`/`removed`), `defaultVersion` bumped
- [ ] Moved APIs handled as fallbacks; all consumers of old FQNs updated
- [ ] Fixture re-pointed, VFS cleared, unversioned rows updated, mocks for layout changes
- [ ] FQN guard and language-level scenarios added
- [ ] `./gradlew cleanTest test` passes; `snakemake_master` failures triaged
- [ ] `CHANGELOG.md` and `docs/snakemake-api/<version>.md` written
