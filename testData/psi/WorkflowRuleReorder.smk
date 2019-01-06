ruleorder: rule1 > rule2 > rule3

def foo():
  ruleorder: tex2pdf_with_bib > tex2pdf_without_bib


# SyntaxError: Expected a descending order of rule names, e.g. rule1 > rule2 > rule3 ...
# only > and identifiers

# todo: invalid