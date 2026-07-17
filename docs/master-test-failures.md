# Pre-existing test failures on a fresh `master` checkout

> **Status:** draft / work-in-progress. The "before" numbers are confirmed; the
> fixture experiment ("after") is marked **PENDING** below and will be filled in once the run
> completes.

## TL;DR

On a **clean checkout of `master`**, `./gradlew test` reports **135 failing Cucumber scenarios**
(out of 3419 testcases). They are **not code regressions** — they are an environmental
**test-fixture gap**: the snakemake library fixture the tests resolve against is *gitignored* and
therefore absent on a fresh checkout. The same 135 failures appear in the 2026.1 port
([PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570)); this document is the
independent, reproducible evidence that they pre-exist that port.

## Why this document exists

1. **Evidence alongside [PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570).** A
   full-suite diff showed master's 135 failures are a *perfect subset* of the port branch's 145
   (`master ⊂ branch`); the port adds only 10 new, port-specific failures and fixes none/breaks none
   of the pre-existing set. Reviewers should not read the 135 as port regressions.
2. **A starting point for a fix.** The failures point at a specific, fixable gap in the test-data
   setup (and its documentation). See [Root cause](#root-cause) and [Proposed fix](#proposed-fix).

## Environment

- Branch point: `master` @ `aa529e6b` (`chore: plugin version update to next minor 2025.2.2`).
- Target platform: `platformType = PC` (PyCharm Community), `platformVersion = 2025.2`.
- Toolchain: **JDK 21** (required — the pinned Gradle 8.13 crashes under newer JDKs).

## How to reproduce

From a clean checkout (no test-data fixture provisioned):

```shell
# JDK 21 is required. On this machine: JAVA_HOME=/path/to/jdk-21
JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JAVA_HOME") \
  ./gradlew cleanTest test -PsnakemakeWrappersRepoPath=testData/wrappers_storage
```

Then extract the failing scenarios from the JUnit XML:

```shell
# every <testcase> with a <failure>/<error> child, as "<feature> :: <scenario>"
python3 extract_failures.py build/test-results/test   # -> 135 failing
```

Notes:
- **`testData` is not a declared input of the `test` task** — always use `cleanTest` when test-data
  changes, or you may get stale cached results.
- **Build-memory gotcha (environment, not the plugin):** on a memory-constrained/daemon-cluttered
  machine `:compileKotlin` can die with `OutOfMemoryError: GC overhead limit exceeded` while
  transforming a large generated method. Give the Kotlin daemon more heap, e.g. append
  `-Pkotlin.daemon.jvmargs=-Xmx4g` (does not modify any tracked file).
- The full suite took **~17 min** here (target platform already downloaded; first run adds a
  hundreds-of-MB download).

The authoritative list of the 135 failing scenarios captured on this commit is in
[`master-failures.txt`](master-failures.txt).

## The 135 failures, by feature

| # | Feature |
|---:|---|
| 57 | Resolve implicitly imported python names |
| 24 | Ensures that fqn used in `snakemake_api.yaml` corresponds to actual resolved reference fqn |
| 12 | Resolve for section names in rules and checkpoints |
| 12 | Inspection warns `min_version` specifying version smaller than the one set in settings |
| 7 | Completion in python part of snakemake file #1 |
| 6 | Resolve for sections/variables in SmkSL injections |
| 6 | Completion for sections/variables in SmkSL injections |
| 4 | This feature is tests for errors/warnings related to implicit symbols |
| 4 | Inspection warns about deprecated/removed keywords, or keywords not introduced yet |
| 1 | Resolve in python part of snakemake file |
| 1 | Inspection if section isn't declared in rule |
| 1 | Inspection for methods from snakemake library |

The set is **dominated by resolution** (resolve / completion / FQN of the `snakemake` package). A
handful (notably the 12 `min_version` inspection scenarios) may be independent of the fixture — see
the experiment.

## Root cause

Cucumber scenarios choose the snakemake library root via the project-setup step
(`src/test/kotlin/features/glue/StepDefs.kt:56-63`):

- `Given a snakemake:<ver> project` → attaches `testData/MockPackages3_smk_<ver>/` — a **trimmed,
  checked-in** per-version API mock. These resolve fine.
- `Given a snakemake project` (unversioned) → attaches `testData/MockPackages3/`, which expects a
  `snakemake` package directory at `testData/MockPackages3/snakemake`.

That directory is **gitignored** (`.gitignore:137`, `testData/MockPackages3/snakemake`) and **absent
on a clean checkout**. Without it, `resolveQualifiedName("snakemake")` returns `[]`, so every
unversioned-project resolve/completion/FQN scenario fails. `DEVELOPER.md` → *Configure Tests* step 2
documents provisioning it by cloning the snakemake repo and symlinking it in.

### Why provisioning it is easy to get wrong (the "zero effect")

An earlier attempt to supply this fixture reportedly had "zero effect." Two traps explain that:

1. **Version.** `snakemake_api.yaml` sets `defaultVersion: "9.9.0"`. The 24 FQN scenarios
   (`ensure_smk_api_function_fqn.feature`) assert *exact* fully-qualified names that only exist in a
   matching source tree — e.g. `snakemake.io.expand`, `snakemake.ioutils.subpath.subpath`,
   `snakemake.ioflags.update`. A fixture cloned at the wrong version leaves those failing.
2. **Layout.** Modern snakemake moved its package into a `src/` layout: the importable package is at
   **`src/snakemake/`**, not repo-root `snakemake/`. `DEVELOPER.md`'s recipe
   (`ln -s ~/snakemake/snakemake snakemake`) points at the *old* root-level path — followed literally
   against a recent clone it creates a **broken symlink**, so the fixture is silently still absent.

Confirmed against a `v9.9.0` checkout: the package lives at `src/snakemake/`, and every FQN the tests
assert is present (`io/__init__.py` exports `ancient, directory, ensure, expand, from_queue, local,
multiext, pipe, protected, repeat, report, service, temp, temporary, touch, unpack`; `ioflags.py`
has `update`/`before_update`; `ioutils/{branch,evaluate,exists,lookup,subpath}.py` present).

## Experiment: does the correct fixture fix it?

**Method.** On the same clean `master`, provision the fixture at the correct version+layout —
symlink `testData/MockPackages3/snakemake` → `<snakemake v9.9.0>/src/snakemake` — then
`cleanTest test` the full suite and diff the failing set against the 135.

**Result: PENDING** (run in progress). To be filled in:

- failures with fixture: `___` (down from 135)
- of the 135, recovered: `___`; residual: `___`
- residual breakdown (which features, and whether fixture-related): `___`

Expected (hypothesis): the resolution-dominated cluster (~120+) recovers; any residual (e.g. the 12
`min_version` scenarios) is flagged as independent.

## Proposed fix

Depends on the experiment, smallest first:

1. **Documentation fix (likely sufficient for the developer path).** Correct `DEVELOPER.md` →
   *Configure Tests* step 2 for the `src/` layout and pin the expected version:
   `ln -s ~/snakemake/src/snakemake testData/MockPackages3/snakemake`, and note the fixture must be
   snakemake **9.9.0** (matching `snakemake_api.yaml`'s `defaultVersion`). This makes a correctly
   set-up developer checkout green; it does **not** make a bare `git clone && ./gradlew test` or a
   clean CI checkout pass on its own.
2. **Green-by-default (maintainer decision).** To make an unprovisioned checkout pass, either vendor
   a trimmed `MockPackages3/snakemake` fixture (it is currently gitignored precisely because the full
   source is large), or automate the clone/symlink in the build/CI. This is a design choice for the
   maintainers and is out of scope for a pure documentation fix.

## CI (GitHub Actions) feasibility — note only, no workflow committed

The repository currently uses **TeamCity** (README badges) and has **no `.github/workflows`**. A GHA
workflow would actually *reproduce* these failures authentically, because a clean CI checkout has no
local symlink — which is useful for demonstration but is the opposite of what you'd want for a green
CI. If the maintainers want CI here, a minimal sketch: `actions/setup-java` (JDK 21) + Gradle cache;
provision the fixture (clone snakemake `9.9.0`, symlink `src/snakemake`) as a step so tests can pass;
scope with Cucumber tags if the full suite is too slow for free runners; ensure the wrappers bundle
(`testData/wrappers_storage`, checked in) is present or run `buildTestWrappersBundle`. Offered as a
follow-up **only if the maintainers want it.**

## Appendix

- Full failing-scenario list: [`master-failures.txt`](master-failures.txt) (135 lines).
- Code pointers: `src/test/kotlin/features/glue/StepDefs.kt:56-63`, `.gitignore:137`,
  `snakemake_api.yaml` (`defaultVersion`), `DEVELOPER.md` → *Configure Tests*.
