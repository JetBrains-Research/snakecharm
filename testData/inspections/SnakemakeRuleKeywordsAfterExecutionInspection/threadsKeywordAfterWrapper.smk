rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    wrapper:
        "dir/wrapper"
    <warning descr="Rule keyword 'threads' isn't allowed after 'wrapper' keyword.">threads</warning>: 8
