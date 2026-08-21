# 135 test failures on a fresh checkout: cause and fix

*A record of one diagnosis, kept so the next person who hits these failures doesn't repeat it.
Reported as [issue #575](https://github.com/JetBrains-Research/snakecharm/issues/575), fixed by
[PR #574](https://github.com/JetBrains-Research/snakecharm/pull/574). Self-contained: this directory
is safe to delete once it stops being useful. The fix it describes lives in `DEVELOPER.md`; the
triage helper it uses lives in `scripts/extract_failures.py`.*

## Problem

On a clean checkout of `master`, `./gradlew test` reports **135 failing Cucumber scenarios** (of
3419). They are not code bugs — they are a **missing test fixture**, and they fail identically on
`master` and on the 2026.1 port branch
([PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570)). This documents the cause and
a verified fix; the problem report is
[issue #575](https://github.com/JetBrains-Research/snakecharm/issues/575).

Reproduce:

```shell
# JDK 21 is required: the pinned Gradle 8.13 crashes under newer JDKs ("Type T not present").
# Point JAVA_HOME at a real JDK 21 and verify — do NOT trust `/usr/libexec/java_home -v 21`,
# which treats 21 as a *minimum* and silently returns a newer JDK when 21 isn't installed.
export JAVA_HOME=/path/to/jdk-21          # e.g. `jenv prefix 21`, or an asdf/SDKMAN path
"$JAVA_HOME/bin/java" -version            # must print 21.x

# PR #572 (merged) makes :buildWrappersBundle skip itself when snakemakeWrappersRepoPath is unset.
# On a checkout without it, #571 bites and you need
#   -PsnakemakeWrappersRepoPath=testData/wrappers_storage
# which is how the numbers below were captured. Unrelated to the 135 failures either way.
./gradlew cleanTest test
python3 scripts/extract_failures.py build/test-results/test   # -> 135 failing
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

Verified end-to-end: the full suite goes from **135 failures to 1**. Two steps —
skipping the second is why provisioning the fixture can appear to have **no effect**:

1. **Provision the fixture at the right version and layout.** Modern snakemake keeps its package
   under `src/`, and the version must match `snakemake_api.yaml`'s `defaultVersion`, which the FQN
   tests assert exact names against (e.g. `snakemake.ioutils.subpath.subpath`). **At the time of this
   diagnosis `defaultVersion` was `9.9.0`**, so that is the version these numbers were produced with:

   ```shell
   git clone --branch v9.9.0 https://github.com/snakemake/snakemake.git ~/snakemake
   ln -sfn ~/snakemake/src/snakemake testData/MockPackages3/snakemake
   ```

   `DEVELOPER.md` deliberately reads the version out of `snakemake_api.yaml` instead of hardcoding it,
   so the setup instructions keep working after a `defaultVersion` bump. This record hardcodes 9.9.0
   on purpose: it documents what was actually run, and should not silently change meaning later.

   Use `-fn`: anyone who followed the old recipe already has a **broken** symlink at that path, and
   a plain `ln -s` aborts with "File exists" and leaves it in place.

   The recipe `DEVELOPER.md` carried before this fix (`ln -s ~/snakemake/snakemake …`) predated the
   `src/` move and creates a broken symlink there.

2. **Let the test sandbox see it.** The IDE test sandbox persists a VFS/index under
   `.sandbox_pycharm` that **`cleanTest` does not clear**. If you add the fixture *after* running the
   tests once — which includes anyone who ran the reproduce command above — that stale index keeps
   reporting the directory's old contents and the failures persist unchanged. Always clear it:

   ```shell
   find .sandbox_pycharm -maxdepth 3 -name system-test -exec rm -rf {} + 2>/dev/null
   ```

   `find` rather than `rm -rf .sandbox_pycharm/*/system-test`, because the sandbox depth varies with
   how tests were launched and with the platform-plugin version (`.sandbox_pycharm/system-test`,
   `.sandbox_pycharm/<ide>/system-test`, `.sandbox_pycharm/<project>/<ide>/system-test` all occur) —
   a glob that misses deletes nothing while appearing to succeed.

## Recommended repository changes

- **`DEVELOPER.md` → Configure Tests, step 2** — fixed in this branch: correct the symlink to
  `src/snakemake`, take the snakemake version from `snakemake_api.yaml`'s `defaultVersion` rather
  than hardcoding it, and document the sandbox-cache gotcha. This is the root of the confusion behind
  PR #570 and PR #573.
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
- Fixture (9.9.0 `src/snakemake`) + cleared sandbox: `failing: 1` (134 of the 135 recovered). The
  survivor is `Resolve implicitly imported python names > Warn about unresolved snakemake variable in
  run section, behaviour differs from scripts`, which the fixture does not address; it is a genuine
  pre-existing failure, not an environmental one.
- Key code: `StepDefs.kt:56-63`, `SmkImplicitPySymbolsProvider.kt`, `CompletionResolveSteps.kt`,
  `snakemake_api.yaml` (`defaultVersion: 9.9.0`), `.gitignore:137`.

## Links

- [Issue #575](https://github.com/JetBrains-Research/snakecharm/issues/575) — the problem report.
- [PR #574](https://github.com/JetBrains-Research/snakecharm/pull/574) — the `DEVELOPER.md` fix and
  this record.
- [PR #570](https://github.com/JetBrains-Research/snakecharm/pull/570) — the 2026.1 port, where these
  135 failures appear too and are easy to mistake for port regressions. They are not.
- [Issue #571](https://github.com/JetBrains-Research/snakecharm/issues/571) /
  [PR #572](https://github.com/JetBrains-Research/snakecharm/pull/572) — the hardcoded
  `snakemakeWrappersRepoPath`, now fixed upstream. Unrelated to these failures, but on a checkout
  predating #572 you hit it first.
- [PR #573](https://github.com/JetBrains-Research/snakecharm/pull/573) — `AGENTS.md`, which summarises
  this gotcha for anyone (or anything) reading the repo cold.
