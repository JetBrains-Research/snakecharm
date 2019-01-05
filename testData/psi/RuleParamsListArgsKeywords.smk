rule foo1:
  input: "iii"
  output: "ooo"

  params: "ppp"
  log: "logs/abc.log"
  resources:
      mem_mb=100

  threads: 50
  priority: 50
  version: "1.0"
  message: "Executing somecommand with {threads} threads on the following files {input}."

  conda:
        "envs/ggplot.yaml"
  benchmark:
         repeat("benchmarks/somecommand/{sample}.tsv", 3)

  group: "mygroup"
  shadow: "shallow"

  wildcard_constraints:
          dataset="\d+"
  shell:
          "somecommand --group {wildcards.group}  < {input}  > {output}"

rule foo2:
  wrapper:
      "0.15.3/bio/bwa/mem"

rule foo3:
  script:
      "path/to/script.py"

rule foo4:
  cwl:
        "https://github.com/common-workflow-language/workflows/blob/fb406c95/tools/samtools-sort.cwl"