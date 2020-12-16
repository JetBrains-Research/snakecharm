rule formatter_preview:
    input:
        "f1",
           "f2",
        "f2",
        "reads/{sample}_1_1.fastq",
        "reads/{sample}_1_2.fastq",
        "reads/{sample}_2_1.fastq", "reads/{sample}_2_2.fastq", "reads/{sample}_3_1.fastq", "reads/{sample}_3_2.fastq",
    output:
        html="qc/fastqc/{sample}.html",
        zip="qc/fastqc/{sample}_smth.zip" # the suffix _smth.zip is necessary for multiqc to find the file. If not using multiqc, you are free to choose an arbitrary filename
    params: ""
    log:
        "logs/smth/{sample}.log"
    threads: 1
    wrapper:
        "0.68.0/bio/smth"