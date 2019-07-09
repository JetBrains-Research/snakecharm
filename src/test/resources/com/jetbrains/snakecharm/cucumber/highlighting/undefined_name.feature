Feature: Yet-undefined name after rules/checkpoints

  Scenario: Name not defined yet
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: rules.ANOTHER_NAME

    rule ANOTHER_NAME:
      input: "in.txt"
    """
    And Undefined name inspection is enabled
    Then I expect inspection error on <rules.ANOTHER_NAME> with message
    """
    This name hasn't been defined yet: ANOTHER_NAME
    """
    When I check highlighting errors

  Scenario: All names are defined
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "a.txt"

    rule ANOTHER_NAME:
      input: rules.NAME
    """
    And Undefined name inspection is enabled
    When I check highlighting errors