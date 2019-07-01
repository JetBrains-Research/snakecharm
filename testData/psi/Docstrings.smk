rule TRIPLE_QUOTED_DOCSTRINGS: """Docstring 1"""
    """Docstring 2"""
    """Docstring 3""" """Docstring 4"""
    """Docstring 5""" input: "input.txt"
    """Docstring 6"""output: "output.txt"

rule DOUBLE_QUOTED_DOCSTRINGS: "Docstring 1"
    "Docstring 2"
    "Docstring 3" "Docstring 4"
    "Docstring 5" input: "input.txt"
    "Docstring 6"output: "output.txt"

rule SINGLE_QUOTED_DOCSTRINGS: 'Docstring 1'
    'Docstring 2'
    'Docstring 3' 'Docstring 4'
    'Docstring 5' input: "input.txt"
    'Docstring 6'output: "output.txt"