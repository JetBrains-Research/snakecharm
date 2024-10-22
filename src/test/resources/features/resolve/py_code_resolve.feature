Feature: Resolve in python part of snakemake file

  Scenario: Resolve any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    repr("foo")
    """
    When I put the caret at repr
    Then reference should resolve to "repr" in "builtins.pyi"


  Scenario: Do not warn about unresolved snakemake variable in python scripts and wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     assert snakemake.input.get("sort", "missing") == "missing"
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors