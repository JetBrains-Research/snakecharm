Feature: Resolve implicitly imported python names
  Resolve runtime magic from snakemake

  Scenario Outline: Resolve at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <symbol_name>("foo")
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
    | ptn | symbol_name | file  |
    | exp | expand      | io.py |
#    | tem | temp        | io.py |
#    | pro | protected   | io.py |

  Scenario Outline: Resolve inside rule parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule all:
      input: <symbol_name>()
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
    | ptn | symbol_name | file  |
    | exp | expand      | io.py |

  Scenario Outline: Resolve inside run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      output: "path/to/output"
      run:
        datasets = <symbol_name>()
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<symbol_name>" in "<file>"

    Examples:
    | ptn | symbol_name | file  |
    | exp | expand      | io.py |


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
