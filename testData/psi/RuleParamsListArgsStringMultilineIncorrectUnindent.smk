rule rule1:
    input:
            "file"
                    "15"
            "_copy"
                 "1"
              ".txt"
            , "another" "file"
                  ".txt"

rule rule2:
    input: "file"
                    "15"
           "_copy"
                 "1"
              ".txt"
           , "another" "file"
                  ".txt"