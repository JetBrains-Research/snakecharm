Feature: 'shadow:' section related inspections
  Scenario: Unknown shadow setting or typo in setting name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output: "output.txt"
        shadow: "parameter"
    """
    And Shadow Settings inspection is enabled
    Then I expect inspection warning on <"parameter"> with message
    """
    Shadow must either be 'shallow', 'full', 'minimal', or True (equivalent to 'full').
    """
    When I check highlighting warnings

  Scenario: No inspection if supported setting name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output: "output.txt"
        shadow: "full"
    """
    And Shadow Settings inspection is enabled
    And Section Multiple Args inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
