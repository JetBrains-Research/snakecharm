rule NAME:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    script:
        "script.py"
    <warning descr="Multiple run or shell keywords are not allowed.">wrapper</warning>:
        "dir/wrapper"
    <warning descr="Multiple run or shell keywords are not allowed.">cwl</warning>:
        "https://github.com/repository/with/file.cwl"
