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