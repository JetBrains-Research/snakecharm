rule foo1:
    run:
        rule = 1
        checkpoint = 2
        print(rule,
checkpoint)

rule foo2:
    run:
        rule = 1
        checkpoint = 2
        print(rule
checkpoint:
    output: touch("output.txt")