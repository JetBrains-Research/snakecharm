configfile: "config.yaml"

# config['FQ_RELATIVE_PATHS'] = collect_relative_paths(
    ""
)

include: "rules/raw_qc.smk"
include: "rules/bismark_align.smk"

rule all:
    input:
        "ddd"
