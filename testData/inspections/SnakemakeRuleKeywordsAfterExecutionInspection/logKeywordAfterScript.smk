rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    script:
        "script_file.py"
    <warning descr="No rule keywords allowed after 'script' keyword.">log</warning>: "log_path"
