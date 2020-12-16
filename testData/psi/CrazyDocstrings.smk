# only first section doc is taken

rule NAME: """Docstring 1"""
    """Docstring 2""" """Docstring 3"""
    """Docstring 4""" input: "input.txt"
    """Docstring 5"""output: "output.txt"

rule boo:
    'doc-2'
    f'doc-{1}'
    "doc-3"
    """doc-4"""
    run:
        for rule in workflow.rules:
            print("RULE", rule.name)
            print("DOCSTRING", rule.docstring)
