localrules: foo1, foo2

def foo():
  localrules: boo1, boo2

# comma and identifier
# SyntaxError: Expected a comma separated list of rules that shall not be executed by the cluster command.
# todo invalid