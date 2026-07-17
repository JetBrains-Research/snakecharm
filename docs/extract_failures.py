#!/usr/bin/env python3
"""Walk build/test-results/test/TEST-*.xml, print sorted 'classname :: name' for
every testcase with a failure/error child. Usage: extract_failures.py <results_dir>"""
import glob, os, sys, xml.etree.ElementTree as ET

results_dir = sys.argv[1] if len(sys.argv) > 1 else "build/test-results/test"
rows = []
total = 0
for f in glob.glob(os.path.join(results_dir, "TEST-*.xml")):
    root = ET.parse(f).getroot()
    for tc in root.iter("testcase"):
        total += 1
        if tc.find("failure") is not None or tc.find("error") is not None:
            cls = tc.get("classname", "")
            rows.append(f"{cls} :: {tc.get('name','')}")
rows.sort()
sys.stderr.write(f"total testcases: {total}, failing: {len(rows)}\n")
print("\n".join(rows))
