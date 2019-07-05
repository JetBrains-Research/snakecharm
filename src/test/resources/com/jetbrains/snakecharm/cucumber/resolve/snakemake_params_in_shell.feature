Feature: Resolve for params in shell section
  Resolve params arguments in shell section

  Scenario Outline: Resolve in shell section in rules
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin)),
        file1="path",
        _file1="path"
      shell: "command {params.<text>}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn         | text          | symbol_name | file         |
      | {params.out | outdir        | outdir      | foo.smk      |
      | {params.xm  | xmx           | xmx         | foo.smk      |
      | {params.fi  | file1         | file1       | foo.smk      |
      | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in shell section in case of 'nested' parameters in rules
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

  Scenario Outline: Resolve in shell section in checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin)),
        file1="path",
        _file1="path"
      shell: "command {params.<text>}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn         | text          | symbol_name | file         |
      | {params.out | outdir        | outdir      | foo.smk      |
      | {params.xm  | xmx           | xmx         | foo.smk      |
      | {params.fi  | file1         | file1       | foo.smk      |
      | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in shell section in case of 'nested' parameters in checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      checkpoint aaaa:
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