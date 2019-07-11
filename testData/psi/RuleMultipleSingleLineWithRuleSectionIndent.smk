rule all1: input: foo1,
                          foo0
rule all2: input: foo1
                          ,foo0
rule all3: input: foo1,
                          foo0,
rule all4: input: foo1
                          foo0
rule all5: input: rule,
                          rule
rule all6: input: rule
                          ,rule
rule all7: input: rule,
                          rule,
rule all8: input: rule
                          rule
rule all9: input: foo1,
                          foo0
5 + 5

rule all10: input: foo1,
                          foo0
onsuccess:
    foo = 2

rule all11: input: foo1,
                          foo0
wildcard_constraints:
    dataset="\d+"
