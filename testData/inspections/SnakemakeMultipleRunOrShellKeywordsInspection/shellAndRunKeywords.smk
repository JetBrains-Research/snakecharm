rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"
    <warning descr="Multiple run or shell keywords are not allowed.">run</warning>:
        print('string')