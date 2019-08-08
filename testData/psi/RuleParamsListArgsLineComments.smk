rule foo:
    shell:
        "echo foo"
        # tod

rule boo:
    shell: "echo boo"

rule soo:
    input:
          "arg",
          # comment
             # comment
       # comment
          "arg" # comment
          # comment
          # comment
     # comment
          , "arg"
          # comment
               # comment
    # comment
# comment

rule doo:
    shell: "echo doo"

rule foo:
    input:
           "echo foo"
           # comment
                    # comment
          # comment
           , "text"
    # comment
               # comment
# comment
           , "text" # comment
           # comment
    # comment
        # comment
  # comment
                           # comment
# comment
rule boo:
    shell: "echo boo"