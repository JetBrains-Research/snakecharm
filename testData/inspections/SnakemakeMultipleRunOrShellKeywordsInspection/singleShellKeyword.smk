rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"