# the checkpoint that shall trigger re-evaluation of the DAG
checkpoint somestep:
    input:
        "samples/{sample}.txt"
    output:
        "somestep/{sample}.txt"
    shell:
        # simulate some output vale
        "echo {wildcards.sample} > somestep/{wildcards.sample}.txt"
