rule aaa:
    output: "foo"
    params:
        configfile = "foo4",
        report = "foo1",
        singularity = "foo2",
        include = "foo5",
        workdir = "foo6",
        wildcard_constraints = "foo7",
        onstart = "foo8",
        onsuccess = "foo9",
        onerror = "foo10",
        localrules = "foo10",
        ruleorder = "foo10",
