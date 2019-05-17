rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"
    <warning descr="No rule keywords allowed after 'shell' keyword.">params</warning>: mem_mb=100
