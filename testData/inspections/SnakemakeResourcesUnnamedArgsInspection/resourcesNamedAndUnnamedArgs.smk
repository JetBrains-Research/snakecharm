rule NAME:
    input: "path/to/input"
    output: "path/to/output"
    resources: <warning descr="Unnamed argument in 'resources' section.">100</warning>, gpu=1, <warning descr="Unnamed argument in 'resources' section.">60</warning>
    shell: "shell command"