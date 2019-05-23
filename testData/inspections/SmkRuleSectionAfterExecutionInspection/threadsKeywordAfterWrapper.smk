rule name:
    input:
        "inputfile.txt"
    output:
        "outputfile.txt"
    wrapper:
        "dir/wrapper"
    <error descr="Rule section 'threads' isn't allowed after 'wrapper' section.">threads: 8</error>
