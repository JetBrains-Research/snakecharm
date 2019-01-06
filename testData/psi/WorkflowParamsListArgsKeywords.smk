configfile: "config.yaml"
report: "report/workflow.rst"
singularity: "docker://continuumio/miniconda3:4.4.10"
include:
    'tex.smrules'
workdir: "path/to/workdir"
wildcard_constraints:
    dataset="\d+"

def foo():
    configfile: "config.yaml"
    include: "path/to/other.snakefile"
