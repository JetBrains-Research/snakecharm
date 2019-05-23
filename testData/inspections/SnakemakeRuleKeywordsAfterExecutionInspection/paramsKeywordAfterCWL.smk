rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    cwl:
        "https://github.com/respository/with/file.cwl"
    <warning descr="No rule keywords allowed after 'cwl' keyword.">params</warning>: mem_mb=100
