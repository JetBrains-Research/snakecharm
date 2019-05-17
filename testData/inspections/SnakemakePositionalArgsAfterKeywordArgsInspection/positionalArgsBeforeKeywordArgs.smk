a = "path/to/input"
b = "path/to/another/file"

rule NAME:
    input: a, b, c="path/to/inputfile"
    output: "path/to/output"
    shell: "shell command"


