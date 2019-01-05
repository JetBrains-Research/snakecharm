rule samtools_sort:
    output: "zoo1",
           "zoo2",
              "zoo3",
                  "zoo4",
           "zoo5",
             "zoo6",
           "zoo7"

    input: "foo1",
      "foo2",
        "foo3"

    input: "boo1",
      "boo2",
        "boo3"