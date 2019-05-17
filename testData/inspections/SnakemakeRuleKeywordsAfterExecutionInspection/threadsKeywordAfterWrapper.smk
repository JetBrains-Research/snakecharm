rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    wrapper:
        "dir/wrapper"
    <warning descr="No rule keywords allowed after 'wrapper' keyword.">threads</warning>: 8
