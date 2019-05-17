rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

rule <warning descr="This rule name is already used by another rule.">NAME</warning>:
    input:
        "outputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"