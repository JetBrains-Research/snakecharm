# 135 test failures on a fresh checkout: cause and fix

## Problem

On a clean checkout of `master`, `./gradlew test` reports **135 failing Cucumber scenarios** (of
3419). They are not code bugs — they are a **missing test fixture**, and they fail identically on
`master` and on the 2026.1 port branch
([PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570)). This documents the cause and
a verified fix.

Reproduce:

```shell
# JDK 21 is required (the pinned Gradle 8.13 crashes under newer JDKs).
JAVA_HOME=$(/usr/libexec/java_home -v 21) \
  ./gradlew cleanTest test -PsnakemakeWrappersRepoPath=testData/wrappers_storage
python3 docs/extract_failures.py build/test-results/test   # -> 135 failing
```

All 135 use the unversioned `Given a snakemake project` step and fail because the `snakemake` API
doesn't resolve. The full list captured on this commit is [`master-failures.txt`](master-failures.txt);
by feature the biggest groups are *Resolve implicitly imported python names* (57), the
`snakemake_api.yaml` FQN check (24), and *Resolve for section names* (12).

## Cause

Cucumber scenarios attach a snakemake library root in `configureSnakemakeProject`
(`src/test/kotlin/features/glue/StepDefs.kt:56-63`):

- `Given a snakemake:<ver> project` → the checked-in `testData/MockPackages3_smk_<ver>/`. **Passes.**
- `Given a snakemake project` (unversioned) → `testData/MockPackages3/`, expecting a `snakemake`
  package at `testData/MockPackages3/snakemake`. **This directory is gitignored (`.gitignore:137`)
  and absent on a clean checkout**, so `resolveQualifiedName("snakemake")` finds nothing and the
  API-symbol provider (`SmkImplicitPySymbolsProvider`) injects nothing — every dependent reference
  resolves to 0 targets.

## Fix

Verified end-to-end: the full suite goes from **135 failures to 0** (`3419/3419` green). Two steps —
skipping the second is why provisioning the fixture can appear to have **no effect**:

1. **Provision the fixture at the right version and layout.** Modern snakemake keeps its package
   under `src/`, and the tests expect version **9.9.0** (it must match `snakemake_api.yaml`'s
   `defaultVersion`; the FQN tests assert exact 9.9.0 names like `snakemake.ioutils.subpath.subpath`):

   ```shell
   git clone --branch v9.9.0 https://github.com/snakemake/snakemake.git ~/snakemake
   ln -s ~/snakemake/src/snakemake testData/MockPackages3/snakemake
   ```

   `DEVELOPER.md`'s current recipe (`ln -s ~/snakemake/snakemake …`) predates the `src/` move and
   creates a broken symlink.

2. **Let the test sandbox see it.** The IDE test sandbox persists a VFS/index under
   `.sandbox_pycharm/<ide>/system-test/` that **`cleanTest` does not clear**. If you add the fixture
   *after* running the tests once, that stale index keeps reporting the directory's old contents and
   the failures persist unchanged. Clear it once:

   ```shell
   rm -rf .sandbox_pycharm/*/system-test
   ```

   On a fresh checkout or clean CI this is unnecessary — the sandbox is built with the fixture
   already present.

## Recommended repository changes

- **`DEVELOPER.md` → Configure Tests, step 2** — fixed in this branch: correct the symlink to
  `src/snakemake`, pin snakemake 9.9.0, and document the sandbox-cache gotcha. This is the root of
  the confusion behind PR #570 and PR #573.
- **Optional (green-by-default):** to make a bare `git clone && ./gradlew test` (and clean CI) pass
  with no manual setup, either vendor a trimmed `MockPackages3/snakemake` fixture (it is gitignored
  today because the full source is large) or automate the clone/symlink in the build. This is a
  maintainer decision, since gitignoring it was deliberate.

## CI (GitHub Actions) note

The repo uses TeamCity (README badges) and has no `.github/workflows`. A clean CI runner has no prior
sandbox, so a workflow that provisions the fixture (clone snakemake 9.9.0, symlink `src/snakemake`)
before `./gradlew test` would pass without any cache-clear. Offered as a follow-up only if
maintainers want it — no workflow is included here.

## Evidence

- Clean checkout: `failing: 135` ([`master-failures.txt`](master-failures.txt)).
- Fixture (9.9.0 `src/snakemake`) + cleared sandbox: `failing: 0` (full suite, all 135 recovered).
- Key code: `StepDefs.kt:56-63`, `SmkImplicitPySymbolsProvider.kt`, `CompletionResolveSteps.kt`,
  `snakemake_api.yaml` (`defaultVersion: 9.9.0`), `.gitignore:137`.
