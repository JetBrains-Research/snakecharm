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

rule <error descr="This rule name is already used by another rule.">NAME</error>:
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

rule <error descr="This rule name is already used by another rule.">ANOTHER_NAME</error>:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "echo ${input} > {output}"

rule <error descr="This rule name is already used by another rule.">NAME</error>:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"

