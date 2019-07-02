rule NAME:
    input: "input1.txt"
    output: touch("output.txt")
    params: a="value"
    resources: threads=4
    <weak_warning descr="Declaration of section 'resources' above overshadows this declaration.">resources: mem_mb=100</weak_warning>
    threads: 4
    <weak_warning descr="Declaration of section 'params' above overshadows this declaration.">params: b="5"</weak_warning>
    shell: "command"