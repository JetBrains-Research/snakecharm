rule r1:
    input: "1"

rule r2:
    input: "1"

checkpoint c1:
    input: "1"

checkpoint c2:
    input: "1"

subworkflow s1:
    configfile: "1"

subworkflow s2:
    configfile: "2"
