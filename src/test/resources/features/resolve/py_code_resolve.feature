Feature: Resolve in python part of snakemake file

  Scenario: Resolve any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    repr("foo")
    """
    When I put the caret at repr
    Then reference should resolve to "repr" in "builtins.pyi"
