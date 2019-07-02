rule NAME:
    input: "input.txt"
    output: "output.txt"
    params: a="value"
    resources: threads=4
    <weak_warning descr="Declaration of section 'params' above overshadows this declaration.">params: b="text"</weak_warning>
    shell: "command"