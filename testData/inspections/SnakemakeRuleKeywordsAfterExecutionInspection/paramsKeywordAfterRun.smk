rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    run:
        print('string')
    <warning descr="No rule keywords allowed after 'run' keyword.">params</warning>: mem_mb=100
