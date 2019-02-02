rule:
    output: report("report/workflow.rst")
    wildcard_constraints:
        dataset="\d+"
    singularity:
          "docker://continuumio/miniconda3:4.4.10"