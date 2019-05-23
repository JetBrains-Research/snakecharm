rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    script:
        "script_file.py"
    <error descr="Rule section 'log' isn't allowed after 'script' section.">log: "log_path"</error>
