# Testing

This is the canonical guide for test setup, execution, fixtures, and result analysis.
Complete [environment setup](../DEVELOPER.md#environment-setup) first so Gradle and the IDE use
the branch's required JDK. Platform-specific investigations live in [porting history](porting/README.md).

## Suite layout

Cucumber/Gherkin features under `src/test/resources/features/` run through
`features.AllCucumberFeaturesTest`; glue is in `src/test/kotlin/features/glue/`.
There is no per-feature test class. Plain JUnit tests also cover lexers, parsers, and other
components. Test data lives under `testData/`.

## Prerequisites and fixture setup

Provision the default Snakemake fixture before running tests that resolve its API.
A fresh checkout lacks it even though the version-specific mocks are checked in.
For IDE feature runs, also follow [Running from IntelliJ IDEA](#running-from-intellij-idea).

### Snakemake fixture

The unversioned `Given a snakemake project` cucumber scenarios resolve the snakemake API against
`testData/MockPackages3/snakemake` (gitignored, absent on a fresh checkout). Provide it by
symlinking the snakemake package source. Two details matter:

* **Version:** it must match `defaultVersion` in `snakemake_api.yaml`, which the FQN tests assert
  against (e.g. `snakemake.ioutils.subpath.subpath`). Read the version from that file rather than
  hardcoding one, so the fixture follows `defaultVersion` when it is bumped.
* **Layout:** modern snakemake keeps its package under `src/`, so the symlink target is
  `src/snakemake` (older releases had it at the repo root).

```shell
# run from the project root
VER=$(awk '/^defaultVersion:/{gsub(/[":]/,"",$2); print $2}' snakemake_api.yaml)
echo "Snakemake version: $VER"
# works whether or not you already cloned snakemake for an earlier version of this recipe
[ -d ~/snakemake ] || git clone https://github.com/snakemake/snakemake.git ~/snakemake
# chained: a bad version must not leave the symlink pointing at the wrong revision
# -fn replaces the broken symlink left by the old recipe
git -C ~/snakemake fetch --tags && git -C ~/snakemake checkout "v$VER" &&
   ln -sfn ~/snakemake/src/snakemake testData/MockPackages3/snakemake
```

Check the result before running the suite — the `&&` chain means `ln` never runs if the git steps
fail, and both a leftover broken symlink and a directory that swallowed the link (`ln` into a real
directory creates `snakemake/snakemake` and exits 0) look like a provisioned fixture:

```shell
ls -l testData/MockPackages3/snakemake/__init__.py
```

If `defaultVersion` changes later, re-point the fixture the same way — the FQN tests will fail
against a stale checkout.

**Gotcha — "zero effect":** the test IDE sandbox persists a VFS/index under `.sandbox_pycharm`
that **`cleanTest` does not clear**. If you add this fixture *after* having already run the tests
once, the stale VFS won't see the new files and the failures persist unchanged. Always clear it
after provisioning the fixture:

```shell
# guarded rather than 2>/dev/null: the sandbox does not exist until you have run the tests once,
# but a removal that genuinely fails must not be silenced -- that lands you right back here
[ -d .sandbox_pycharm ] && find .sandbox_pycharm -maxdepth 3 -name system-test -exec rm -rf {} +
```

Use `find`, not `rm -rf .sandbox_pycharm/*/system-test`: the sandbox sits at a different depth
depending on how tests were launched (`.sandbox_pycharm/system-test` for run configurations made
from the old hand-written template, `.sandbox_pycharm/<ide>/system-test` and `.sandbox_pycharm/<project>/<ide>/system-test`
for the gradle task, varying by platform-plugin version), and a glob that misses simply deletes
nothing while looking like it worked.

## Running from Gradle

### Full suite and cache invalidation

```shell
./gradlew cleanTest test
```

**`testData` is NOT a declared input of the `test` task.** After editing any feature or
test-data file, run `./gradlew cleanTest test` — plain `test` may serve stale cached results.

`cleanTest` invalidates test results, not the sandbox VFS. Clear the latter only when fixture
files changed as described in [fixture setup](#snakemake-fixture), not as a routine first step.

### Focused Cucumber runs

Add a `@here` tag above the chosen `Feature:` line (or above a single `Scenario:` /
`Scenario Outline:`) and run

```shell
CUCUMBER_TAGS='@here' ./gradlew cleanTest test --tests "features.AllCucumberFeaturesTest"
```

`test` forwards `CUCUMBER_TAGS` to cucumber's `cucumber.filter.tags`, composed with the runner's
own `not @ignore`; `--tests` skips the plain JUnit tests, which the tag filter cannot. Revert the
tag afterwards, and pass the variable inline as above rather than `export`ing it, or the next
"full" run quietly runs only `@here`. It turns a 25-minute suite into a ~60-second one. On a
branch without that passthrough, set `tags = "not @ignore and @here"` in
`AllCucumberFeaturesTest.kt` instead and revert that too.

### Test JVM memory

`SNAKECHARM_TEST_HEAP` overrides the test JVM's maximum heap. Left unset, the run uses the
target IDE's VM options (2 GB in the recorded 2026.2 runs). Increase it when needed for a
diagnostic run, considering the machine's available memory:

```shell
SNAKECHARM_TEST_HEAP=8g ./gradlew cleanTest test --tests "features.AllCucumberFeaturesTest"
```

This is separate from the [Kotlin daemon heap](../DEVELOPER.md#build-and-packaging).

## Running from IntelliJ IDEA

Install `Cucumber for Java` and `Gherkin`; `Cucumber+` is optional for editing.
Restart the IDE after installation. If every feature reports `Unimplemented substep definition`,
check that the Cucumber/Gherkin plugins are enabled and disable the conflicting
`Substeps IntelliJ Plugin` if installed.

A `Cucumber Java` run configuration (gutter icon / context menu on a `.feature` file) launches the
JVM itself, not through Gradle, so it gets nothing the IntelliJ Platform Gradle Plugin attaches to
the `test` task. That is two separate things, and the IDE takes each from a different place:

```
java  <VM options>                         -classpath <jars>               <main> <args>
      from the run configuration            always built by the IDE from
      = @build/tmp/ideTestRun/jvm.args      the module dependencies (.iml)
```

* **JVM options** — ~50 `--add-opens`, `java.system.class.loader`, the test sandbox paths,
  `idea.python.helpers.path`, ... Without them: `IllegalAccessError: ... module java.desktop does
  not export sun.awt`. The `prepareIdeTestRun` task writes them into a Java argfile (and builds
  the test sandbox and test wrappers bundle).
* **Classpath** — the IDE builds `-classpath` from the module dependencies, which a Gradle sync
  imports. `build.gradle.kts` adds the jars only `test` has to `testRuntimeOnly`, during sync only
  (see the end of this section).

Neither can replace the other: a Gradle sync never imports a task's JVM options, and a `-cp` in
the argfile is overridden by the `-classpath` the IDE appends after the VM options. Using Gradle's
classpath instead would also drop the IDE's own runner jars (Cucumber/JUnit support, `idea_rt`)
and run Gradle's packaged sandbox jar instead of the classes the IDE just compiled.

**The `Cucumber Java` run configuration template is already checked in** as
`.run/Template Cucumber Java.run.xml`, and the IDE picks it up on project open — nothing to set
up. Every `Feature: …` / `Scenario: …` configuration created from a `.feature` file inherits it.
What it sets, for reference:

* `VM options`: `@$PROJECT_DIR$/build/tmp/ideTestRun/jvm.args` — the argfile above, and nothing
  else (no hand-written `-Didea.*` paths, which would point at the wrong sandbox);
* `Before launch`: `Build`, then the Gradle task `prepareIdeTestRun` (project `snakecharm`), so
  the argfile, the test sandbox and the test wrappers bundle are fresh for every run;
* module `snakecharm.test`, program arguments `--plugin teamcity`, shorten command line: none.

Two things the shared template does not do for you:

* Configurations created *before* you got it (e.g. from an older hand-edited template, with
  `-Didea.config.path=…` VM options) keep their old settings — delete them and let the IDE
  re-create them from the template.
* If the IDE does not pick the file up, or you prefer a per-user setup, apply the same settings by
  hand as a fallback: `Run | Edit Configurations... | Edit configuration templates... |
  Cucumber Java`, then set `VM options` and add `Before launch` → `+` → `Run Gradle task` →
  `prepareIdeTestRun` as listed above.

`Glue` may stay empty: `src/test/resources/cucumber.properties` sets `cucumber.glue`. Re-import
the Gradle project after pulling this: the IDE's test classpath needs the forced `kotlin-stdlib`
(an older one first on it hangs project setup with "Debug metadata version mismatch") and the
platform's test-runtime jars and the jars of all bundled plugins, which `build.gradle.kts` adds
only during IDE sync (otherwise: `ClassNotFoundException:
com.intellij.platform.settings.local.SettingsControllerMediator`, or `Missing extension point:
Pythonid.pythonSdkFlavor`). The bundled-plugin jars are taken from the `test` task's own classpath
(its jars inside the IDE distribution), so they follow whatever the gradle plugin puts there.
"Only during sync" means the `idea.sync.active` system property, which the IDE sets to `true` for
a Gradle sync only — never for `./gradlew test` or for Gradle tasks the IDE runs. Adding the jars
outside sync would reorder `test`'s classpath and break it. So after changing that part of the
build script, **re-sync**; a rebuild is not enough.

## Writing scenarios and fixtures

### Version-specific mocks

Snakemake API mocks live under `testData/MockPackages3_smk_<version>/snakemake`.
For a new version, create that directory and copy only the required API files, such as files
whose API changed. Select it with `Given a snakemake:<version> project`.
The unversioned step uses the [default fixture](#snakemake-fixture).

After changing mock files, clear the sandbox VFS using the fixture-setup procedure.
`cleanTest` alone does not make an existing VFS index see the changed fixture.

### Scenario isolation

Every scenario asks IntelliJ's light-fixture framework for a test project by handing it a
`LightProjectDescriptor` — the object that says
which Python SDK and library roots the project needs. The framework hands back the *same* project
as long as it is given the same descriptor, and rebuilds it when the descriptor changes. Before
#577 `StepDefs` constructed a fresh descriptor per scenario, so scenarios were mostly insulated
from each other by accident. It caches descriptors now, and has to — on 2026.2 an SDK is a
workspace-model entity, so building a second mock SDK with the same name logs "symbolic id already
exists", which `TestLoggerFactory` turns into ~1070 failed scenarios. Once the project is shared,
everything held by a project-level *service* — framework enabled/disabled, settings, the
configured SDK — survives into the next scenario. **Write steps that set the project state they
need rather than assume a fresh project's defaults.**
`Given a snakemake with disabled framework project` is the cautionary example: it never disabled
anything, it only skipped the enabling, and it passed for years purely because each scenario used
to start from a clean project.

### Completion assertions

A light fixture may auto-insert a sole completion variant and open no lookup. If the test
needs to inspect a popup, use `I invoke autocompletion popup without inserting a single variant`;
if it tests insertion, assert the resulting text. Do not depend on unrelated Python completion
variants keeping the popup open. See [the 2026.3 finding](porting/2026.3.md#completion-assertions).

## Troubleshooting

### Missing Snakemake resolution

On a fresh checkout, broad failures resolving `snakemake` often mean the gitignored
`testData/MockPackages3/snakemake` fixture is absent. Version-specific mocks can still resolve,
which makes the failure look selective. Check the fixture's version, its `src/snakemake`
layout, and the sandbox VFS using [fixture setup](#snakemake-fixture). PR #574 records the case.

### Cluster failure messages first

`TestLoggerFactory` promotes error-level logs to failed scenarios. One platform error can
therefore fail hundreds of unrelated tests. Group the first lines of `<failure message=...>`
in `build/test-results/test/*.xml` before investigating features individually.

The Gradle console reports scenario names and exception classes, but may omit the actual failure
messages. An empty grep for a message in the console is not evidence that the problem disappeared.
The [2026.2 serialization failure](porting/2026.2.md#serialization-abi) is a worked example.

### Missing or demoted highlighting

`When I check highlighting <level>s` calls `CodeInsightTestFixture.checkHighlighting`,
which reports only the requested severity (plus
errors) and *silently discards the rest* — so a highlight whose severity dropped from `WARNING`
to `WEAK WARNING` fails with the exact same `missing (…)` message as one that is not produced at
all. Before hunting for a suppression, dump what is actually there: add a temporary step calling
`fixture.doHighlighting()` and print each `HighlightInfo`'s `severity`, `type`, range,
`description` and `inspectionToolId`. One run replaces a sandbox debugging session — that is how
#584 was resolved. Platform bumps move these mappings: on 2026.1
`ProblemHighlightType.LIKE_UNKNOWN_SYMBOL` renders as `HighlightInfoType.INFO` (weak warning),
where 2025.2 gave a plain warning. **Fixing such a scenario by re-labelling its step costs
coverage**, for the same reason: moving `warning`s to `weak warning`s stops it asserting anything
at WARNING level, and in a scenario without `ignoring extra highlighting` that assertion was the
guard against stray warnings. Use `I check highlighting warnings and weak warnings`, which asks
for both.

### Diagnostics moved between inspections

**A platform bump can move a check between inspections, and the scenario then passes vacuously.**
`Given <X> inspection is enabled` fails loudly on an inspection that was *renamed*
(`fail("Unknown inspection:…")`), but says nothing when the inspection still exists and merely
stopped owning the diagnostic the scenario is about. The check for `expand(" ", **1)` moved from
`PyArgumentListInspection` to `PyTypeCheckerInspection` in 2026.2, which is why one scenario lost
its warning — and why its sibling, which asserts `expand(" ", **wildcards)` produces *no* warning,
went on passing while guarding nothing at all. Trace the message to its owner rather than guessing:
grep the message text in the platform's `messages/*.properties` for its bundle key, then grep the
extracted plugin jars for the class that references that key, in both the old and new IDE. Two
greps beat a type-inference theory — the key was renamed
`INSP.expected.dict.got.type` → `INSP.type.checker.unpack.expected.mapping`, which names the new
owner outright. A scenario asserting "no warning" is worth re-checking after any bump for exactly
this reason.

### Slow runs

Every figure below is a single measurement of the full suite on 2026.1, so read the band,
not the ordering — these differ by machine and load as much as by what
they are nominally measuring:

| run | time |
|---|---|
| warm Gradle daemon | ~25 min |
| cold daemon, sandbox VFS intact | 1h48m; ~95 min extrapolated from an earlier partial run |
| cold daemon, straight after clearing the sandbox VFS | 1h24m |
| memory-constrained machine, swapping | 3h52m |

In these measurements, cold full runs took roughly **1.5–2 hours**. They do not isolate the cost
of clearing the VFS, so use [fixture changes](#snakemake-fixture), not timing, to decide when to
clear it. The 3h52m run was on a 16 GB laptop with several GB of swap in use and healthy GC.
On macOS, check `sysctl vm.swapusage` when diagnosing a slow run. Prefer the focused `@here`
recipe while iterating; these measurements are not a runtime guarantee on other branches or machines.

## Analyzing results

Gradle prints each failing scenario and a `N tests completed, M failed` summary; for a run you are
watching, that is the report. To compare which scenarios failed before and after a change,
capture each run and reduce it
to a sorted list of scenario names. For root causes, group the XML failure messages as described
under [failure clustering](#cluster-failure-messages-first). Capture a run and extract its names
before the change using `before` in place of `after`, then repeat after the change and compare:

```shell
set -o pipefail
./gradlew cleanTest test 2>&1 | tee /tmp/after.log
sed -n '/ > /s/ FAILED$//p' /tmp/after.log | sort -u > /tmp/after.names
diff /tmp/before.names /tmp/after.names
```

The `/ > /` address keeps only scenario lines, skipping Gradle's own `> Task :test FAILED` (no
space before its `>`); `-n` with `p` then prints just the lines the substitution changed. Check
the resulting line count against `M failed`. Use `cleanTest test`, not plain `test`: `testData` is
not a declared input of the task, so an unchanged-looking build can report `:test UP-TO-DATE`,
print no scenario lines at all, and leave you diffing against an empty file that reads as
"everything got fixed". HTML reports are turned off in `build.gradle.kts` (Windows cannot handle
some Cucumber scenario names), so what a finished run leaves on disk is the JUnit XML under
`build/test-results/test/`. That holds the same information if you need a run whose console output
you no longer have — but note it is written when the `test` task *ends*.

**An all-green run prints no count at all**, because Gradle prints `N tests completed` only on
failure. So a run in progress looks identical to a hung one, and — worse — **a truncated run looks
identical to a good one**: `BUILD SUCCESSFUL` says only that nothing failed, and `CUCUMBER_TAGS`
makes an empty run easy to reach, since a tag expression matching no scenario exits 0 just as
loudly as a full suite. Before reporting a run as green, read the count out of the XML (`tests=`
summed over `build/test-results/test/*.xml`): compare with a full run on the same revision and
platform. Historical measurements were **3420** tests across 125 suites after #570/#577, and
**3423** on the recorded 2026.3 EAP port. Neither is a
permanent expected total; record the actual count, filters, revision, and platform with your result.
A `CUCUMBER_TAGS` still exported in your shell, an unintended filter, or a leftover
`tags = "not @ignore and @here"` in `AllCucumberFeaturesTest` is the
usual cause of a short one. While a run is going, the live signals are the test JVM's accumulating
CPU time (`ps -o time=`) and the mtime of `build/test-results/test/binary/in-progress-results-generic.bin`;
`jstat -gc` tells you whether a
quiet stretch is a slow scenario or a GC death spiral.
