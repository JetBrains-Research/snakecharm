rule samtools_sort:
    input:
        "foo1",
        "foo2"
    output: "boo1", "boo2"