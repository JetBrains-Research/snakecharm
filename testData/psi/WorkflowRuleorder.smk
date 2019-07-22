ruleorder: rule1

ruleorder: rule1 > rule2 > rule3

ruleorder:
  rule1 > rule2 > doo >
  roo > ho

ruleorder: rule1 > rule2 > doo >
  roo > ho

ruleorder:
    rule1 > rule2
        > rule3 > rule4

def foo():
  ruleorder: tex2pdf_with_bib > tex2pdf_without_bib
