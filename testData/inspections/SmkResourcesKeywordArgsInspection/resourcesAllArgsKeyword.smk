rule NAME:
    input: "path/to/input"
    output: "path/to/output"
    resources: mem_mb=100, gpu=1, time=60
    shell: "shell command"