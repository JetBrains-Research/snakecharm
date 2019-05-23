rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    cwl:
        "https://github.com/respository/with/file.cwl"
    <error descr="Rule section 'params' isn't allowed after 'cwl' section.">params: mem_mb=100</error>
