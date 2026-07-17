# Pre-existing test failures on a fresh `master` checkout

> **Status:** draft / work-in-progress, shared to start a conversation. The demonstration (135
> failures) is confirmed and reproducible. The diagnosis below is the result of a bounded set of
> controlled experiments; the exact code-level mechanism is narrowed but not yet pinned, and would
> benefit from a maintainer with a debugger.

## TL;DR

On a **clean checkout of `master`**, `./gradlew test` reports **135 failing Cucumber scenarios**
(out of 3419 testcases). They are **not code regressions** and — importantly — they are **not caused
by a missing test fixture** (a hypothesis we tested thoroughly and **disproved**, see below). Every
failure is in a scenario that sets up the project with the **unversioned** `Given a snakemake
project` step; the **versioned** `Given a snakemake:<ver> project` scenarios pass with the *same*
library content. The same 135 failures appear in the 2026.1 port
([PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570)); this document is the
independent, reproducible evidence that they pre-exist that port.

## Why this document exists

1. **Evidence alongside [PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570).** A
   full-suite diff showed master's 135 failures are a *perfect subset* of the port branch's 145
   (`master ⊂ branch`); the port adds only 10 new, port-specific failures and breaks none of the
   pre-existing set.
2. **A starting point for a fix — and a warning against a wrong one.** The obvious guess ("the
   gitignored snakemake fixture is missing on a fresh checkout") is **wrong**; provisioning the
   fixture does not help. This document records the experiments so nobody repeats them, and points
   at where the real cause lives.

## Environment

- Branch point: `master` @ `aa529e6b` (`chore: plugin version update to next minor 2025.2.2`).
- Target platform: `platformType = PC` (PyCharm Community), `platformVersion = 2025.2`.
- Toolchain: **JDK 21** (required — the pinned Gradle 8.13 crashes under newer JDKs).

## How to reproduce

From a clean checkout:

```shell
# JDK 21 is required.
JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JAVA_HOME") \
  ./gradlew cleanTest test -PsnakemakeWrappersRepoPath=testData/wrappers_storage
```

Extract the failing scenarios from the JUnit XML:

```shell
python3 extract_failures.py build/test-results/test   # -> 135 failing
```

Notes:
- **`testData` is not a declared input of the `test` task** — always use `cleanTest` when test-data
  changes, or you may get stale cached results.
- **Build-memory gotcha (environment, not the plugin):** on a memory-constrained/daemon-cluttered
  machine `:compileKotlin` can die with `OutOfMemoryError: GC overhead limit exceeded`. Give the
  Kotlin daemon more heap, e.g. append `-Pkotlin.daemon.jvmargs=-Xmx4g` (modifies no tracked file).
- The full suite took **~17 min** here (target platform already downloaded).

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

Every one of these is a scenario that configures the project with the **unversioned** `Given a
snakemake project` step. All of them fail because `snakemake`'s API symbols don't resolve
(`multiResolve` returns 0 results where 1 is expected).

## What actually distinguishes pass from fail

Cucumber scenarios choose the snakemake library root in `configureSnakemakeProject`
(`src/test/kotlin/features/glue/StepDefs.kt:56-63`):

- `Given a snakemake:<ver> project` → attaches `testData/MockPackages3_smk_<ver>/`. **These pass.**
- `Given a snakemake project` (unversioned) → attaches `testData/MockPackages3/`. **These fail.**

The cleanest illustration is a single scenario outline,
`features/resolve/implicit_py_symbols_resolve.feature` → *Resolve at top-level*, which contains both
kinds of row for the **same symbol resolving to the same file**:

```
| snakemake:9.3.0 | exp | expand() | expand | __init__.py |   # PASSES
| snakemake       | exp | expand() | expand | __init__.py |   # FAILS
```

Same symbol, same expected target, same underlying library files — only the **project-version
declaration** differs. So the failure is a property of the **unversioned project setup path**, not
of any fixture or of the snakemake sources.

## The fixture hypothesis — and why it's wrong

`testData/MockPackages3/snakemake` is gitignored (`.gitignore:137`) and absent on a clean checkout,
and `DEVELOPER.md` → *Configure Tests* step 2 tells you to clone snakemake and symlink it there. The
natural hypothesis is therefore "the unversioned scenarios fail because that fixture is missing."
**We tested this four ways. All produced the identical 135 failures — zero change:**

| Provisioning of `testData/MockPackages3/snakemake` | Failing scenarios |
|---|---|
| Absent (clean checkout) | 135 |
| Symlink → real snakemake **9.9.0** source (`src/snakemake`, matching `snakemake_api.yaml` `defaultVersion`) | 135 (identical set) |
| Real 9.9.0 source **copied into the project** (rules out external-path/VFS indexing) | 135 (identical set) |
| **Known-good checked-in mock content** (`MockPackages3_smk_9.3.0/snakemake`) copied to the bare path | 81/81 tagged still fail |

The last row is decisive: the *exact* library content that resolves fine under the
`MockPackages3_smk_9.3.0` root fails under the bare `MockPackages3` root. **It is not the content,
the version, the layout, or the presence of the fixture.** This also explains the earlier
"supplying the fixture had zero effect" observation — that observation was correct; the fixture is
simply not the cause.

(For the record: modern snakemake did move its package to a `src/` layout, so `DEVELOPER.md`'s
`ln -s ~/snakemake/snakemake …` recipe is stale and would create a broken symlink. Worth fixing
independently — but it is **not** what makes these 135 tests fail.)

## Where the cause actually is (narrowed, not yet pinned)

The divergence is entirely between the versioned and unversioned project-setup paths, which differ
only in the configured snakemake **version / language level**. Implicit API symbols (what these
scenarios resolve against) are built by `SmkImplicitPySymbolsProvider`, which resolves
`snakemake.io` / `snakemake.ioflags` / `snakemake.ioutils` by qualified name against the project SDK
(`SmkImplicitPySymbolsProvider.kt:189-208, 627-640`). For a versioned project this succeeds; for the
unversioned project it yields nothing, so no implicit symbols are injected and every dependent
reference resolves to 0 targets.

The unversioned project's language level defaults via `snakemake_api.yaml` (`defaultVersion:
"9.9.0"`). The open question — best answered interactively with a debugger in the resolve path — is
**why the unversioned setup fails to resolve/collect the API while an explicit `snakemake:<ver>`
setup with identical files succeeds.** Candidate areas: how `configureSnakemakeProject` establishes
the language version for the unversioned case, and how `SmkSupportProjectSettings` /
`SmkImplicitPySymbolsProvider` gate collection on it.

## Proposed next step

This is **not** a documentation or fixture-provisioning fix. The right next step is an interactive
debug session (breakpoint in `SmkImplicitPySymbolsProvider` collection + the `multiResolve` used by
`CompletionResolveSteps.resolve`) on one unversioned scenario, comparing it against the equivalent
`snakemake:9.3.0` row that passes. Help from a maintainer familiar with the resolve/implicit-symbol
code would make this quick.

Independent of the above, `DEVELOPER.md`'s snakemake-symlink recipe should be corrected for the
`src/` layout — but note that doing so will **not** fix these tests.

## CI (GitHub Actions) feasibility — note only, no workflow committed

The repository currently uses **TeamCity** (README badges) and has **no `.github/workflows`**. A GHA
workflow would reproduce these failures authentically on a clean runner (useful for demonstration),
but since the cause is not the fixture, "provision the fixture in CI" would **not** turn them green.
Offered as a follow-up only if maintainers want it.

## Appendix

- Full failing-scenario list: [`master-failures.txt`](master-failures.txt) (135 lines).
- Code pointers: `src/test/kotlin/features/glue/StepDefs.kt:56-63` (project setup),
  `SmkImplicitPySymbolsProvider.kt:189-208,627-640` (API symbol collection),
  `CompletionResolveSteps.kt` (the `resolve`/`multiResolve` assertions), `snakemake_api.yaml`
  (`defaultVersion`), `.gitignore:137`, `DEVELOPER.md` → *Configure Tests*.
- Experiments were run at snakemake `v9.9.0` and with the checked-in `MockPackages3_smk_9.3.0`
  content; all four provisioning variants above yielded the identical failing set.
