Feature: Check Resolve
  Resolve runtime magic from snakemake

  Scenario: Resolve any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    print("foo")
    """
    When I put the caret at prin
    Then reference should resolve to "print" in "builtins.pyi"

  Scenario Outline: Resolve snakemake io expand in smk file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <method_name>("foo")
    """
    When I put the caret at <ptn>
    Then reference should resolve to "<method_name>" in "<file>"

    Examples:
    | ptn | method_name | file  |
    | exp | expand      | io.py |
#    | tem | temp        | io.py |
#    | pro | protected   | io.py |