Feature: Resolve for params in shell section
  Resolve params arguments in shell section

  Scenario Outline: Resolve for params in shell section in rules/checkpoints
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

  Scenario Outline: Resolve for params in shell section in case of 'nested' parameters in rules/checkpoints
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

  Scenario Outline: Resolve for params in run section with shell call in rules/checkpoints
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

  Scenario Outline: Resolve for params in run section with shell call in case of 'nested' parameters in rules/checkpoints
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

  Scenario Outline: Resolve in shell section in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      <section>: kwd1="arg1", kwd2="arg2"
      shell: "command {<section>.<text>}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn           | text | symbol_name | file    | section   |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | input     |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | output    |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | resources |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | log       |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | input     |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | output    |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | resources |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | log       |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | input     |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | output    |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | resources |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | log       |

  Scenario Outline: Resolve for params in run section with shell call in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> aaaa:
      <section>: kwd1="arg1", kwd2="arg2"
      run:
        shell("command {<section>.<text>}")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn           | text | symbol_name | file    | section   |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | input     |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | output    |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | resources |
      | rule       | {<section>.kw | kwd1 | kwd1        | foo.smk | log       |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | input     |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | output    |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | resources |
      | rule       | {<section>.kw | kwd2 | kwd2        | foo.smk | log       |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | input     |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | output    |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | resources |
      | checkpoint | {<section>.kw | kwd1 | kwd1        | foo.smk | log       |

  Scenario Outline: Resolve for params in run section with shell call in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        <section>:
          xmx=os
        run:
          shell("command {<section>.xmx<suffix>}")
      """
    When I put the caret after {<section>.xm
    Then reference should resolve to "xmx" in "foo.smk"
    Examples:
      | rule_like  | suffix   | section   |
      | rule       | .path    | input     |
      | rule       | [path]   | input     |
      | rule       | {'path'} | input     |
      | checkpoint | .path    | input     |
      | checkpoint | [path]   | input     |
      | checkpoint | {'path'} | input     |
      | rule       | .path    | output    |
      | rule       | [path]   | output    |
      | rule       | {'path'} | output    |
      | checkpoint | .path    | output    |
      | checkpoint | [path]   | output    |
      | checkpoint | {'path'} | output    |
      | rule       | .path    | resources |
      | rule       | [path]   | resources |
      | rule       | {'path'} | resources |
      | checkpoint | .path    | resources |
      | checkpoint | [path]   | resources |
      | checkpoint | {'path'} | resources |
      | rule       | .path    | log       |
      | rule       | [path]   | log       |
      | rule       | {'path'} | log       |
      | checkpoint | .path    | log       |
      | checkpoint | [path]   | log       |
      | checkpoint | {'path'} | log       |

  Scenario Outline: Resolve for params in shell section in case of 'nested' parameters in rules/checkpoints
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      import os
      <rule_like> aaaa:
        <section>:
          xmx=os
        shell: "command {<section>.xmx<suffix>}"
      """
    When I put the caret after {<section>.xm
    Then reference should resolve to "xmx" in "foo.smk"
    Examples:
      | rule_like  | suffix   | section   |
      | rule       | .path    | input     |
      | rule       | [path]   | input     |
      | rule       | {'path'} | input     |
      | checkpoint | .path    | input     |
      | checkpoint | [path]   | input     |
      | checkpoint | {'path'} | input     |
      | rule       | .path    | output    |
      | rule       | [path]   | output    |
      | rule       | {'path'} | output    |
      | checkpoint | .path    | output    |
      | checkpoint | [path]   | output    |
      | checkpoint | {'path'} | output    |
      | rule       | .path    | resources |
      | rule       | [path]   | resources |
      | rule       | {'path'} | resources |
      | checkpoint | .path    | resources |
      | checkpoint | [path]   | resources |
      | checkpoint | {'path'} | resources |
      | rule       | .path    | log       |
      | rule       | [path]   | log       |
      | rule       | {'path'} | log       |
      | checkpoint | .path    | log       |
      | checkpoint | [path]   | log       |
      | checkpoint | {'path'} | log       |

  Scenario Outline: resolve for sections without keyword arguments in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      shell: "command {<text>}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn   | text    | symbol_name | file    |
      | rule       | {inp  | input   | input       | foo.smk |
      | rule       | {thre | threads | threads     | foo.smk |
      | checkpoint | {inp  | input   | input       | foo.smk |
      | checkpoint | {thre | threads | threads     | foo.smk |

  Scenario Outline: resolve for sections without keyword arguments in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      run:
        shell("command {<text>}")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn   | text    | symbol_name | file    |
      | rule       | {inp  | input   | input       | foo.smk |
      | rule       | {thre | threads | threads     | foo.smk |
      | checkpoint | {inp  | input   | input       | foo.smk |
      | checkpoint | {thre | threads | threads     | foo.smk |

  Scenario Outline: no resolve for sections without keyword arguments not present in rule in shell section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      shell: "command {<text>}"
    """
    When I put the caret after <ptn>
    Then reference should not resolve
    Examples:
      | rule_like  | ptn  | text      |
      | rule       | {out | output    |
      | rule       | {lo  | log       |
      | rule       | {ver | version   |
      | rule       | {res | resources |
      | rule       | {par | params    |
      | checkpoint | {out | output    |
      | checkpoint | {lo  | log       |
      | checkpoint | {ver | version   |
      | checkpoint | {res | resources |
      | checkpoint | {par | params    |

  Scenario Outline: no resolve for sections without keyword arguments not present in rule in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      run:
        shell("command {<text>}")
    """
    When I put the caret after <ptn>
    Then reference should not resolve
    Examples:
      | rule_like  | ptn  | text      |
      | rule       | {out | output    |
      | rule       | {lo  | log       |
      | rule       | {ver | version   |
      | rule       | {res | resources |
      | rule       | {par | params    |
      | checkpoint | {out | output    |
      | checkpoint | {lo  | log       |
      | checkpoint | {ver | version   |
      | checkpoint | {res | resources |
      | checkpoint | {par | params    |

  Scenario Outline: resolve for shell section with a dot after section name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      output: "output"
      resources: a=1
      log: "file.txt", another="another_file.txt"
      params: b="b", c="c"
      version: "1.0"
      threads: 8
      shell: "{<section>.}"
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn   | section   | symbol_name | file    |
      | rule       | "{in  | input     | input       | foo.smk |
      | rule       | "{out | output    | output      | foo.smk |
      | rule       | "{res | resources | resources   | foo.smk |
      | rule       | "{lo  | log       | log         | foo.smk |
      | rule       | "{par | params    | params      | foo.smk |
      | rule       | "{ver | version   | version     | foo.smk |
      | rule       | "{thr | threads   | threads     | foo.smk |
      | checkpoint | "{in  | input     | input       | foo.smk |
      | checkpoint | "{out | output    | output      | foo.smk |
      | checkpoint | "{res | resources | resources   | foo.smk |
      | checkpoint | "{lo  | log       | log         | foo.smk |
      | checkpoint | "{par | params    | params      | foo.smk |
      | checkpoint | "{ver | version   | version     | foo.smk |
      | checkpoint | "{thr | threads   | threads     | foo.smk |

  Scenario Outline: resolve for run section with a dot after section name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      output: "output"
      resources: a=1
      log: "file.txt", another="another_file.txt"
      params: b="b", c="c"
      version: "1.0"
      threads: 8
      run:
        shell("{<section>.}")
    """
    When I put the caret after <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"
    Examples:
      | rule_like  | ptn   | section   | symbol_name | file    |
      | rule       | "{in  | input     | input       | foo.smk |
      | rule       | "{out | output    | output      | foo.smk |
      | rule       | "{res | resources | resources   | foo.smk |
      | rule       | "{lo  | log       | log         | foo.smk |
      | rule       | "{par | params    | params      | foo.smk |
      | rule       | "{ver | version   | version     | foo.smk |
      | rule       | "{thr | threads   | threads     | foo.smk |
      | checkpoint | "{in  | input     | input       | foo.smk |
      | checkpoint | "{out | output    | output      | foo.smk |
      | checkpoint | "{res | resources | resources   | foo.smk |
      | checkpoint | "{lo  | log       | log         | foo.smk |
      | checkpoint | "{par | params    | params      | foo.smk |
      | checkpoint | "{ver | version   | version     | foo.smk |
      | checkpoint | "{thr | threads   | threads     | foo.smk |

