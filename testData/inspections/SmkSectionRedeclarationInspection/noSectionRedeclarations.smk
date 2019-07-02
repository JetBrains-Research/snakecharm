rule NAME:
    input: "input.txt"
    output: "output.txt"
    resources: threads=4
    params: a="value"
    script: "script.py"