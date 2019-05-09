Feature: Ensure that resolve works in python part of snakemake file

  Scenario: Resolve any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    print("foo")
    """
    When I put the caret at prin
    Then reference should resolve to "print" in "builtins.pyi"
