a = "path/to/input"
b = "path/to/another/file"

rule NAME:
    input: a, b
    output: "path/to/output"
    shell: "shell command"


