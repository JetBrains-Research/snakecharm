rule aaaa:
    input: "path/to/input"
    output: "path/to/output"
    shell: "shell command"

rule bbbb:
    input: rules.aaaa