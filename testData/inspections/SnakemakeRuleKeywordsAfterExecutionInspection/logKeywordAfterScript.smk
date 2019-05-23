rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    script:
        "script_file.py"
    <warning descr="Rule keyword 'log' isn't allowed after 'script' keyword.">log</warning>: "log_path"
