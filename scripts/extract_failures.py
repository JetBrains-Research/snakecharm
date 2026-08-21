#!/usr/bin/env python3
"""List the failing scenarios from a Gradle test run.

Walks TEST-*.xml in a JUnit results directory and prints 'classname :: name' for every
testcase with a <failure> or <error> child, sorted. The count goes to stderr, so the list
can be redirected to a file or diffed between two runs.
"""
import argparse, glob, os, sys, xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(
    description=__doc__,
    formatter_class=argparse.RawDescriptionHelpFormatter,
)
parser.add_argument(
    "results_dir", nargs="?", default="build/test-results/test",
    help="directory holding the JUnit TEST-*.xml files (default: %(default)s)",
)
args = parser.parse_args()

files = glob.glob(os.path.join(args.results_dir, "TEST-*.xml"))
if not files:
    # Without this, an aborted build or a typo'd path reports "failing: 0" — a green run.
    sys.exit(f"error: no TEST-*.xml under {args.results_dir!r}; did the test task run?")
rows = []
total = 0
for f in files:
    root = ET.parse(f).getroot()
    for tc in root.iter("testcase"):
        total += 1
        if tc.find("failure") is not None or tc.find("error") is not None:
            cls = tc.get("classname", "")
            rows.append(f"{cls} :: {tc.get('name','')}")
rows.sort()
sys.stderr.write(f"total testcases: {total}, failing: {len(rows)}\n")
if rows:  # else print() would emit a blank line, so a green run wouldn't diff as an empty file
    print("\n".join(rows))
