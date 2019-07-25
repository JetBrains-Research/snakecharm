rule NAME:
    "Docstring 1"
    input: "input.txt"
    'Docstring 2'
    output: "output.txt"

rule ANOTHER_NAME:
    "Docstring 1"
    input:
      "input.txt"
    'Docstring 2'
    output: "output.txt"