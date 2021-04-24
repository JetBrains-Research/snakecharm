rule all:
    input:
        txt = 'out2.txt'

some_var: str = 'generate_out'

rule:
    name: some_var
    output: 'out1.txt'
    shell: '''echo {rule} > {output}'''


rule name1:
    name: "name2"
    input: rules.generate_out.output
    output: 'out2.txt'
    shell: '''echo {rule} > {output}'''

# NameError in ...: name 'not_defined_var' is not defined
rule:
    name: not_defined_var

# TypeError in ... : attribute name must be string, not 'builtin_function_or_method'
rule:
    name: print

# TypeError in ... : attribute name must be string, not 'function'
rule name1:
    name: lambda x: "name2"

