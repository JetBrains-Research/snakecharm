rule samtools_sort:
    input:
        input="in.bam"
    output: output="in.bam"