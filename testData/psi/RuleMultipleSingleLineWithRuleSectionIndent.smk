rule all1: input: 'foo1',
                          'foo0'
rule all2: input: 'foo2'

rule all3: input: 'foo1'
                          ,'foo0'
rule all4: input: 'foo2'

rule all5: input: 'foo1',
                          'foo0',
rule all6: input: 'foo2'

rule all7: input: 'foo1'
                          'foo0'
rule all8: input: 'foo2'

rule all9: input: 'foo1',
                          'foo0'
5 + 5

rule all10: input: 'foo1',
                          'foo0'
onsuccess:
    print("Workflow finished, no error")
