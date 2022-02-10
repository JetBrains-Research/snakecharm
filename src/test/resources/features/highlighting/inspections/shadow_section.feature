Feature: 'shadow:' section related inspections
  Scenario: Unknown shadow setting or typo in setting name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output: "output.txt"
        shadow: "parameter"
    """
    And SmkShadowSettingsInspection inspection is enabled
    Then I expect inspection warning on <"parameter"> with message
    """
    Shadow must either be 'shallow', 'full', 'minimal', 'copy-minimal', or True (equivalent to 'full').
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
    And SmkShadowSettingsInspection inspection is enabled
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
