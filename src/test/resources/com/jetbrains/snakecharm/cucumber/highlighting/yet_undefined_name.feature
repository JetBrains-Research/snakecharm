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
    Then I expect inspection warning on <rules.ANOTHER_NAME> with message
    """
    This name hasn't been defined yet: ANOTHER_NAME
    """
    When I check highlighting warnings

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
    Then I expect no inspection warning
    When I check highlighting warnings


  Scenario: Unresolved name isn't undefined
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule ANOTHER:
      input: rules.NAME
    """
    And Undefined name inspection is enabled
    And Unresolved reference inspection is enabled
    Then I expect inspection warning on <NAME> with message
    """
    Cannot find reference 'NAME' in 'rules'
    """
    When I check highlighting warnings
