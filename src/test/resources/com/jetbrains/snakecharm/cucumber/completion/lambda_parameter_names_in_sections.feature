Feature: Completion for lambda parameter names in specific sections

  Scenario: Completion in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      params: lambda 
    """
    When I put the caret after lambda
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards       |