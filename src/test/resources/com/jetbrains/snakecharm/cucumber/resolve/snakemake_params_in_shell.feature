Feature: Resolve for params in shell section
  Resolve params arguments in shell section

  Scenario Outline: Resolve in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin))
      shell: "command {params.<text>}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn         | text          | symbol_name | file         |
      | {params.out | outdir        | outdir      | foo.smk      |
      | {params.xm  | xmx           | xmx         | foo.smk      |

    Scenario Outline: Resolve in shell section in case of 'nested' parameters
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      import os
      rule aaaa:
        input: "path/to/input"
        output: "path/to/output"
        params:
          xmx=os
        shell: "command {params.xmx<suffix>}"
      """
      When I put the caret after {params.xm
      Then reference should resolve to "xmx" in "foo.smk"
      Examples:
        | suffix   |
        | .path    |
        | [path]   |
        | {'path'} |


