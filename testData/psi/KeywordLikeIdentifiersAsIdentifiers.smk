import rule
import subworkflow
import workdir, wildcard_constraints
from snakemake.utils import report

rule foo: run:
    rule = 1
    checkpoint = 1
    configfile = 2
    report = 3
    wildcard_constraints = 4