rule all:
  output: "aaa1",
  
rule samtools_sort:
    output: "boo1",
       "boo2",
    input:
        "foo1",
    output: "zoo1",
         "zoo2",
    output: "foo3",