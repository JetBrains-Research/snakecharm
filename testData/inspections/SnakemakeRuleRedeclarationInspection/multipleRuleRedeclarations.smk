rule NAME:
    input:
        "inputfile.txt"

rule ANOTHER_NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

rule <warning descr="This rule name is already used by another rule.">NAME</warning>:
    input:
        "input.txt"
    output:
        "output.txt"
    shell:
        "cat {input} > {output}"

rule DIFFERENT_NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

rule <warning descr="This rule name is already used by another rule.">ANOTHER_NAME</warning>:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "echo ${input} > {output}"

rule <warning descr="This rule name is already used by another rule.">NAME</warning>:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

