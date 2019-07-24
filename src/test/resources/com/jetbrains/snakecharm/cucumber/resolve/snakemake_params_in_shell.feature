Feature: Resolve for params in shell section
  Resolve params arguments in shell section

  Scenario Outline: Resolve in shell section in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
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
    | rule_like  | ptn         | text          | symbol_name | file         |
    | rule       | {params.out | outdir        | outdir      | foo.smk      |
    | rule       | {params.xm  | xmx           | xmx         | foo.smk      |
    | rule       | {params.fi  | file1         | file1       | foo.smk      |
    | rule       | {params._fi | _file1        | _file1      | foo.smk      |
    | checkpoint | {params.out | outdir        | outdir      | foo.smk      |
    | checkpoint | {params.xm  | xmx           | xmx         | foo.smk      |
    | checkpoint | {params.fi  | file1         | file1       | foo.smk      |
    | checkpoint | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in shell section in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        input: "path/to/input"
        output: "path/to/output"
        params:
          xmx=os
        shell: "command {params.xmx<suffix>}"
      """
    When I put the caret after {params.xm
    Then reference should resolve to "xmx" in "foo.smk"
    Examples:
    | rule_like  | suffix   |
    | rule       | .path    |
    | rule       | [path]   |
    | rule       | {'path'} |
    | checkpoint | .path    |
    | checkpoint | [path]   |
    | checkpoint | {'path'} |

  Scenario Outline: Resolve in run section with shell call in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      input: "path/to/input"
      output: "path/to/output"
      params:
        outdir=lambda wildcards, output: os.path.dirname(str(output)),
        xmx=lambda wildcards: str(800 // int(wildcards.bin)),
        file1="path",
        _file1="path"
      run:
        shell("command {params.<text>}")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | rule_like  | ptn         | text          | symbol_name | file         |
      | rule       | {params.out | outdir        | outdir      | foo.smk      |
      | rule       | {params.xm  | xmx           | xmx         | foo.smk      |
      | rule       | {params.fi  | file1         | file1       | foo.smk      |
      | rule       | {params._fi | _file1        | _file1      | foo.smk      |
      | checkpoint | {params.out | outdir        | outdir      | foo.smk      |
      | checkpoint | {params.xm  | xmx           | xmx         | foo.smk      |
      | checkpoint | {params.fi  | file1         | file1       | foo.smk      |
      | checkpoint | {params._fi | _file1        | _file1      | foo.smk      |

  Scenario Outline: Resolve in run section with shell call in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        input: "path/to/input"
        output: "path/to/output"
        params:
          xmx=os
        run:
          shell("command {params.xmx<suffix>}")
      """
    When I put the caret after {params.xm
    Then reference should resolve to "xmx" in "foo.smk"
    Examples:
      | rule_like  | suffix   |
      | rule       | .path    |
      | rule       | [path]   |
      | rule       | {'path'} |
      | checkpoint | .path    |
      | checkpoint | [path]   |
      | checkpoint | {'path'} |