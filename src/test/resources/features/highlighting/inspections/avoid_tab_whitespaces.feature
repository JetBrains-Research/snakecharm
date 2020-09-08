Feature: Inspection warns about using TAB as whitespace.

  Scenario: Using TAB whitespace
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    rule boo:
    	input: "in"
    """
    When SmkAvoidTabWhitespace inspection is enabled
    Then I expect inspection weak warning on pattern <\n\t> with message
    """
    Tab character detected. PEP-8 recommends to use spaces in Python like code.
    """
    When I check highlighting weak warnings