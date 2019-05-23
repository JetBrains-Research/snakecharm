rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    shell:
        "cat {input} > {output}"
    <error descr="Rule section 'params' isn't allowed after 'shell' section.">params: mem_mb=100</error>
