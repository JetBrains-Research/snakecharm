rule NAME:
    input: "path/to/input"
    output: "path/to/output"
    resources: <error descr="Resources have to be named (e.g. 'threads=4').">100</error>
    shell: "shell command"