rule rule1:
    input:
           arg1,
                  arg2,
                arg3 # incorrect unindent

rule rule1a:
    input:
           arg1,
                  arg2,
           arg3 # correct unindent

rule rule2:
    input:
             arg1 +
                    arg2 +
                 arg3 # incorrect unindent

rule rule2a:
    input:
             arg1 +
                    arg2 +
             arg3 # correct unindent

rule rule3:
    input:
             arg1 +
                    arg2
                 + arg3 # incorrect unindent

rule rule3a:
    input:
             arg1 +
                    arg2
             + arg3 # correct unindent

rule rule4:
    input:
             arg1
                  if condition1 or
               condition2 else arg2 # incorrect unindent

rule rule4a:
    input:
             arg1
                  if condition1 or
             condition2 else arg2 # correct unindent
