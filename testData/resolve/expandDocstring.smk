"""
>>>exp<caret>
"""

rule NAME:
    input:
         "path/to/input"
    output:
          "path/to/output"
    shell:
         "shell command"