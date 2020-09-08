envvars:
    "SOME_VARIABLE"

rule aaa:
    params:
        envvars = "foo10"