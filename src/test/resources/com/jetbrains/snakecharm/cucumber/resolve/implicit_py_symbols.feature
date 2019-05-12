Feature: Resolve implicitly imported python names
  Resolve runtime magic from snakemake

  Scenario Outline: Resolve at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
    | ptn | text          | symbol_name | file         |
    | exp | expand()      | expand      | io.py        |
    | tem | temp()        | temp        | io.py        |
    | dir | directory()   | directory   | io.py        |
    | dir | directory()   | directory   | io.py        |
    | pro | protected()   | protected   | io.py        |
    | tou | touch()       | touch       | io.py        |
    | dyn | dynamic()     | dynamic     | io.py        |
    | un  | unpack()      | unpack      | io.py        |
    | anc | ancient()     | ancient     | io.py        |
    | she | shell()       | __new       | shell.py     |
    | con | config        | config      | workflow.py  |
    | con | config["a"]   | config      | workflow.py  |
    | ru  | rules         | rules       | workflow.py  |
    | ru  | rules.foo     | rules       | workflow.py  |
    | inp | input         | input       | builtins.pyi |

  Scenario Outline: Also available on top-level at runtime, but not API
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn | text          | symbol_name | file         |
      | wor | workflow      | workflow    | workflow.py  |

  Scenario Outline: Not-resolved at top-level
     Given a snakemake project
     Given I open a file "foo.smk" with text
     """
     <text>
     """
     When I put the caret at <ptn>
     Then reference should not resolve

     Examples:
     | ptn | text        |
     | out | output.foo  |
     | par | params      |
     | wil | wildcards   |


  Scenario Outline: Resolve inside rule parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
    | ptn    | text        | symbol_name | file        |
    | exp    | expand()    | expand      | io.py       |
    | con    | config["a"] | config      | workflow.py |
    | rules  | rules.foo   | rules       | workflow.py |
    | she    | shell()     | __new       | shell.py    |

  Scenario Outline: Resolve inside run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      output: "path/to/output"
      run:
        <text>
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
      | ptn    | text        | symbol_name | file        |
      | exp    | expand()    | expand      | io.py       |
      | she    | shell()     | __new       | shell.py    |
      | con    | config["a"] | config      | workflow.py |
      | rules  | rules.foo   | rules       | workflow.py |
# TODO: implement:
#      | inp    | input[0]    | input       | io.py       |
#      | out    | output.foo  | output      | io.py       |
#      | par    | params      | params      | xx.py       |
#      | wil    | wildcards   | wildcards   | xx.py       |

  Scenario: Resolve also works inside call args
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: directory(expand("{dataset}/dir", dataset=[]))
    """
    When I put the caret at exp
    Then reference should resolve to "expand" in "io.py"

  Scenario Outline: Implicit resolve is off in python dialects files
    Given a snakemake project
    Given I open a file "foo.<ext>" with text
    """
    <symbol_name>()
    """
    When I put the caret at <ptn>
    Then reference should not resolve

    Examples:
    | ptn | symbol_name | ext |
    | exp | expand      | py  |
    | exp | expand      | pyi |
