Feature: Inspection for multiple settings in 'shadow' section
  Scenario: Multiple settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        shadow: "full", "shallow"
    """
    And Shadow Multiple Settings inspection is enabled
    Then I expect inspection error on <"full"> with message
    """
    Only one argument after shadow section is permitted.
    """
    And I expect inspection error on <"shallow"> with message
    """
    Only one argument after shadow section is permitted.
    """
    When I check highlighting errors