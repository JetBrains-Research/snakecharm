rule rule_314_2:<fold text='...'>
    input: key1="value", key2="value", key3="value"
    output:
        "value1",
        "value2"
    log:
        key1="value"
    message:
        <fold text='"..."'>"aaaa"
        "aaaa"
        "aaaa"
        "aaaa"</fold>
    params:
        key1="value",
        key2="value"
    conda: "foo.yaml"</fold>
