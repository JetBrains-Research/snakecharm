# scripts/

Small developer utilities that support working on the plugin. Nothing here is part of the build or
the shipped plugin — the Gradle build never invokes these, and removing one breaks nothing but the
convenience it provides.

## `extract_failures.py`

Lists the failing scenarios from a Gradle test run. The Cucumber suite is large (~3400 scenarios) and
HTML reports are disabled in `build.gradle.kts` (Windows can't handle some Cucumber names), so the
practical way to see what failed is to parse the JUnit XML that `./gradlew test` leaves in
`build/test-results/test/`.

```shell
./gradlew test
python3 scripts/extract_failures.py                      # defaults to build/test-results/test
python3 scripts/extract_failures.py path/to/results      # or point it somewhere else
python3 scripts/extract_failures.py --help
```

It prints one sorted `classname :: scenario` line per failing testcase on **stdout**, and the summary
(`total testcases: 3419, failing: 0`) on **stderr** — so you can redirect the list to a file without
the count landing in it. That split is what makes it useful for comparing two runs:

```shell
python3 scripts/extract_failures.py > /tmp/before.txt
# ... change something, re-run the suite ...
python3 scripts/extract_failures.py > /tmp/after.txt
diff /tmp/before.txt /tmp/after.txt
```

That comparison is how the failures documented in [`docs/missing-snakemake-fixture/`](../docs/missing-snakemake-fixture/)
were shown to be identical on `master` and on the 2026.1 port branch, rather than regressions
introduced by the port.

It exits non-zero if the results directory contains no `TEST-*.xml`, rather than reporting zero
failures — a mistyped path, or a run that never started, should not look like a green suite. It
cannot tell a *partially* completed run from a finished one; the `total testcases:` line on stderr is
the check for that.
