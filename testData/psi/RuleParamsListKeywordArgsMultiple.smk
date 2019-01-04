rule samtools_sort:
    input:
        int1="i1.bam",
        int2="i2.bam"
    output:
        out1="out1.bam", out2="out2.bam"