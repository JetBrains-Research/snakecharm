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

  Scenario: Resolve snakemake io expand in smk file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    expand("foo")
    """
    When I put the caret at exp
    Then reference should resolve to "expand" in "io.py"