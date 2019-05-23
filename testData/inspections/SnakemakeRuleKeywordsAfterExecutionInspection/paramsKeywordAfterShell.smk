rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"
    <warning descr="Rule keyword 'params' isn't allowed after 'shell' keyword.">params</warning>: mem_mb=100
