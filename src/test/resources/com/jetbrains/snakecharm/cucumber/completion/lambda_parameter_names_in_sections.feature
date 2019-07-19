Feature: Completion for lambda parameter names in specific sections

  Scenario: Completion in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      input: lambda w
    """
    When I put the caret after lambda w
    Then I invoke autocompletion popup, select "wildcards" lookup item and see a text:
    """
    rule rule1:
      input: lambda wildcards: 
    """

  Scenario: Completion in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule rule1:
      params: lambda # purely for this test because I can't insert space into a step definition
    """
    When I put the caret at #
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards |
      | input     |
      | output    |
      | threads   |
      | resources |