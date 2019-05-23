a = "path/to/input"
b = "path/to/another/file"

rule NAME:
    input:
        c="path/to/inputfile",
        <error descr="Positional argument follows keyword argument.">a</error>,
        <error descr="Positional argument follows keyword argument.">b</error>,
        d="input/path"
    output: "path/to/output"
    shell: "shell command"


