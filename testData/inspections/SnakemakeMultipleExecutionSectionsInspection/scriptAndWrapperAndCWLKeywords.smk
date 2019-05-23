rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    script:
        "script.py"
    <error descr="Multiple run or shell sections are not allowed.">wrapper:
        "dir/wrapper"</error>
    <error descr="Multiple run or shell sections are not allowed.">cwl:
        "https://github.com/repository/with/file.cwl"</error>
