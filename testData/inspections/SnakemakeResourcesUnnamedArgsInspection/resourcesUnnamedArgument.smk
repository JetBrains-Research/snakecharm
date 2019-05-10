rule NAME:
    input: "path/to/input"
    output: "path/to/output"
    resources: <warning descr="Only keyword arguments allowed in 'resources' section (e.g. 'threads=4')">100</warning>
    shell: "shell command"