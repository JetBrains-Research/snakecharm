Feature: Completion for lambda parameter names in specific sections

  Scenario Outline: Completion in input section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      input: lambda w
    """
    When I put the caret after lambda w
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> rule1:
      input: lambda wildcards: 
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion in params section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
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
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion in params section with tail text for the last parameter
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      params: lambda wildcards, input, output, resources, #
    """
    When I put the caret at #
    And I invoke autocompletion popup, select "threads" lookup item and see a text:
    """
    <rule_like> rule1:
      params: lambda wildcards, input, output, resources, threads: #
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No completion in params section with tail text for first parameters
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      params: lambda wildcards,
    """
    When I put the caret after ,
    And I invoke autocompletion popup, select "input" lookup item and see a text:
    """
    <rule_like> rule1:
      params: lambda wildcards,input
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
