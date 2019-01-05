rule samtools_sort:
    input:
         "foo1",
         "foo2"
    output: "boo1", "boo2"
    output: "doo1",
       "doo2"
    output: "zoo1",
           "zoo2",
    output: "zzzzzz"