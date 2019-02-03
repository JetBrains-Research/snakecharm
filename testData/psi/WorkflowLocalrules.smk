localrules: foo0

localrules: foo1, foo2

localrules:
    rule1, boo, doo,
    soo, goo

localrules: rule1, boo, doo,
    soo, goo

def foo():
  localrules: boo1, boo2