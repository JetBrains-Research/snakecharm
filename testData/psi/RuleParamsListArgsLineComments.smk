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

rule boo:
    params: a = 30,
          #comment
            b = "text",
                        c = print(30)
    shell: "echo boo" # comment

rule boo:
    params: a = 30,
              #comment
            b = "text",
                        c = print(30)
    shell: "echo boo" # comment

rule foo2:
    input: # comment
# comment
         "echo foo",
        "incorrectly unindented text"
# end of rule comment
configfile: "config.yaml"

# config['FQ_RELATIVE_PATHS'] = collect_relative_paths(
    ""
)

include: "rules/raw_qc.smk"
include: "rules/bismark_align.smk"

rule foo:
    input: "text"

rule doo:
    shell: "echo foo"
    # end of file comment