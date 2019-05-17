a = "path/to/input"
b = "path/to/another/file"

rule NAME:
    input:
        c="path/to/inputfile",
        <warning descr="Syntax error: positional argument follows keyword argument.">a</warning>,
        <warning descr="Syntax error: positional argument follows keyword argument.">b</warning>,
        d="input/path"
    output: "path/to/output"
    shell: "shell command"


