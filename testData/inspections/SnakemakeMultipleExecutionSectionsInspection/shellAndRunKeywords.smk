rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"
    <error descr="Multiple run or shell sections are not allowed.">run:
        print('string')</error>