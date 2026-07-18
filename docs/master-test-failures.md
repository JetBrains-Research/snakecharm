# Pre-existing test failures on a fresh `master` checkout — cause & fix

> **Status:** resolved. Cause identified and the fix verified end-to-end (full suite goes from 135
> failures to **0**). See [The fix](#the-fix).

## TL;DR

On a **clean checkout of `master`**, `./gradlew test` reports **135 failing Cucumber scenarios**
(of 3419). They are **not code regressions** — they are the **absent test fixture**
`testData/MockPackages3/snakemake` (gitignored, so missing on a fresh checkout). Every failing
scenario uses the unversioned `Given a snakemake project` step, which resolves the `snakemake` API
against that directory.

**Provisioning the fixture correctly fixes all 135** (verified: full suite → 3419/3419 green). "Correctly"
has two parts, and missing the second is why an earlier attempt seemed to have *"zero effect"*:

1. **Right content/version/layout:** clone snakemake **9.9.0** (matching `snakemake_api.yaml`'s
   `defaultVersion`) and symlink/copy its **`src/snakemake`** to `testData/MockPackages3/snakemake`.
2. **Make the sandbox VFS see it:** on a fresh checkout this is automatic, but if you add the fixture
   *after* a prior test run already built the IDE sandbox, its persisted VFS/index is stale and the
   new files are invisible — you must `rm -rf .sandbox_pycharm/<ide>/system-test`. **`cleanTest` does
   not clear this.**

The same 135 failures appear in the 2026.1 port
([PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570)); this is the independent,
reproducible evidence that they pre-exist that port.

## Why this document exists

1. **Evidence alongside [PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570).** A
   full-suite diff showed master's 135 failures are a *perfect subset* of the port branch's 145
   (`master ⊂ branch`); the port adds only 10 new, port-specific failures and breaks none of the 135.
2. **A precise fix**, including the non-obvious sandbox-cache step that turns a correct fixture from
   "no effect" into "all green."

## Environment

- Branch point: `master` @ `aa529e6b`.
- Target platform: `platformType = PC` (PyCharm Community), `platformVersion = 2025.2`.
- Toolchain: **JDK 21** (the pinned Gradle 8.13 crashes under newer JDKs).

## Reproduce the failures

From a clean checkout (no fixture provisioned):

```shell
JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JAVA_HOME") \
  ./gradlew cleanTest test -PsnakemakeWrappersRepoPath=testData/wrappers_storage
python3 docs/extract_failures.py build/test-results/test   # -> 135 failing
```

Build-memory gotcha (environment, not the plugin): if `:compileKotlin` dies with
`OutOfMemoryError: GC overhead limit exceeded`, add `-Pkotlin.daemon.jvmargs=-Xmx4g` (modifies no
tracked file). The authoritative 135-scenario list captured on this commit:
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

All use the unversioned `Given a snakemake project` step. They fail because `snakemake`'s API
symbols don't resolve (`multiResolve` returns 0 where 1 is expected).

## Root cause

Cucumber scenarios choose the snakemake library root in `configureSnakemakeProject`
(`src/test/kotlin/features/glue/StepDefs.kt:56-63`):

- `Given a snakemake:<ver> project` → attaches the checked-in `testData/MockPackages3_smk_<ver>/`.
- `Given a snakemake project` (unversioned) → attaches `testData/MockPackages3/`, which expects a
  `snakemake` package at `testData/MockPackages3/snakemake`.

That directory is **gitignored** (`.gitignore:137`) and **absent on a clean checkout**. The implicit
API-symbol provider (`SmkImplicitPySymbolsProvider`) resolves `snakemake` / `snakemake.io` /
`snakemake.ioutils` by qualified name against the project SDK; with the directory missing (or unseen),
`resolveQualifiedName("snakemake")` returns nothing, so no symbols are injected and every dependent
reference in the unversioned scenarios resolves to 0 targets.

### The "zero effect" trap (why provisioning can look like it does nothing)

The cucumber tests use a **persisted IDE sandbox** (`.sandbox_pycharm/<ide>/system-test/{caches,index}`)
whose VFS/index **survives `cleanTest`**. If the sandbox was built while `MockPackages3/snakemake` was
absent, its VFS keeps reporting that directory's *old* contents, so a fixture you add afterward is
invisible to resolution — the tests still fail exactly as before. This was verified directly by
instrumenting the provider: before clearing the cache the bare root reported `children=[peppy]` and
`snakemake=null` on disk-present files; after `rm -rf …/system-test` it reported
`children=[peppy, snakemake]` and resolution succeeded.

## The fix

Verified end-to-end: applying both steps takes the full suite from **135 failures to 0**
(`total testcases: 3419, failing: 0`).

1. **Provision the fixture at the right version + layout.** Modern snakemake keeps its package under
   `src/`, so:
   ```shell
   git clone --branch v9.9.0 https://github.com/snakemake/snakemake.git ~/snakemake
   ln -s ~/snakemake/src/snakemake  testData/MockPackages3/snakemake
   ```
   Version **9.9.0** matches `snakemake_api.yaml`'s `defaultVersion` (the FQN tests assert exact
   9.9.0 names, e.g. `snakemake.ioutils.subpath.subpath`). `DEVELOPER.md`'s current recipe
   (`ln -s ~/snakemake/snakemake …`) predates the `src/` move and would create a **broken symlink**.
2. **Clear the sandbox VFS if it predates the fixture.** On a fresh checkout / clean CI this is
   unnecessary (the sandbox is built with the fixture already present). But if you provisioned the
   fixture after already running tests once:
   ```shell
   rm -rf .sandbox_pycharm/*/system-test        # cleanTest does NOT do this
   ```

## Recommended repository changes

- **`DEVELOPER.md` → Configure Tests, step 2:** fix the symlink to `~/snakemake/src/snakemake`, pin
  snakemake **9.9.0**, and add the "clear `.sandbox_pycharm/<ide>/system-test` if you add the fixture
  after a prior run" note. (This is the root of the confusion for both PR #570 and PR #573.)
- **Optional (green-by-default):** to make a bare `git clone && ./gradlew test` and clean CI pass
  without manual setup, either vendor a trimmed `MockPackages3/snakemake` fixture (it's gitignored
  today because the full source is large) or automate the clone/symlink in the build. Maintainer's
  call, since the gitignore was deliberate.

## CI (GitHub Actions) feasibility — note only, no workflow committed

The repo uses **TeamCity** (README badges) and has **no `.github/workflows`**. A clean CI checkout has
no prior sandbox, so a workflow that provisions the fixture (clone snakemake 9.9.0, symlink
`src/snakemake`) before `./gradlew test` would go green — no cache-clear needed. Offered as a
follow-up only if maintainers want it.

## Appendix

- Full failing-scenario list (clean checkout): [`master-failures.txt`](master-failures.txt) (135).
- Verification: full suite with the fixture (9.9.0 `src/snakemake`) + cleared sandbox → `failing: 0`.
- Code pointers: `StepDefs.kt:56-63` (project setup), `SmkImplicitPySymbolsProvider.kt` (API symbol
  collection via `resolveQualifiedName`), `CompletionResolveSteps.kt` (the resolve assertions),
  `snakemake_api.yaml` (`defaultVersion: 9.9.0`), `.gitignore:137`, `DEVELOPER.md` → Configure Tests.
