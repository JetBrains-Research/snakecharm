rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

rule <error descr="This rule name is already used by another rule.">NAME</error>:
    input:
        "outputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"