rule rule1:
    input:
           "file1.txt",
                  "file2.txt"
                  , "file3.txt",
           "file4.txt",
           "file5.txt" # correct indentation
    shell: "text"

rule rule2:
    input:
           "file1.txt",
                  "file2.txt"
                  , "file3.txt"
           "file4.txt",
        "file5.txt" # incorrect indentation
    shell: "text"


rule rule3:
    input:
           "file1.txt",
                  "file2.txt"
                  , "file3.txt"
           "file4.txt",
    "file5.txt" # parsed as a docstring
    shell: "text"

rule rule4:
    input:
           "file1.txt",
                  "file2.txt"
                  , "file3.txt"
           "file4.txt",
"file5.txt" # incorrect indentation
    shell: "text"
