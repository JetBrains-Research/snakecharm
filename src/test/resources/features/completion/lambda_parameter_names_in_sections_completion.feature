Feature: Completion for lambda parameter names in specific sections

  Scenario Outline: Completion in input and group section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: lambda w #
    """
    When I put the caret after lambda w
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> rule1:
      <section>: lambda wildcards:  #
    """
    Examples:
      | section | rule_like  |
      | input   | rule       |
      | input   | checkpoint |
      | group   | rule       |
      | group   | checkpoint |

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

  Scenario Outline: No completion with tail text for first parameters in params section
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

  Scenario Outline: Completion in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      threads: lambda # purely for this test because I can't insert space into a step definition
    """
    When I put the caret at #
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards |
      | input     |
      | attempt   |
    And completion list shouldn't contain:
      | output    |
      | resources |
      | threads   |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion in threads section with tail text for the last parameter
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      threads: lambda wildcards, input, #
    """
    When I put the caret at #
    And I invoke autocompletion popup, select "attempt" lookup item and see a text:
    """
    <rule_like> rule1:
      threads: lambda wildcards, input, attempt: #
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No completion with tail text for first parameters in threads section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      threads: lambda wildcards,
    """
    When I put the caret after ,
    And I invoke autocompletion popup, select "input" lookup item and see a text:
    """
    <rule_like> rule1:
      threads: lambda wildcards,input
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      resources: a=lambda # purely for this test because I can't insert space into a step definition
    """
    When I put the caret at #
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards |
      | input     |
      | threads   |
      | attempt   |
    And completion list shouldn't contain:
      | output    |
      | resources |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion in resources section with tail text for the last parameter
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      resources: a=lambda wildcards, input, threads, #
    """
    When I put the caret at #
    And I invoke autocompletion popup, select "attempt" lookup item and see a text:
    """
    <rule_like> rule1:
      resources: a=lambda wildcards, input, threads, attempt: #
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No completion with tail text for first parameters in resources section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      resources: a=lambda wildcards,
    """
    When I put the caret after ,
    And I invoke autocompletion popup, select "input" lookup item and see a text:
    """
    <rule_like> rule1:
      resources: a=lambda wildcards,input
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No completion for lambda invocations
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: (lambda wildcards, : wildcards)("")
    """
    When I put the caret after ,
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | wildcards |
      | input     |
      | output    |
      | resources |
      | threads   |
      | attempt   |
    Examples:
      | section              | rule_like  |
      | benchmark            | rule       |
      | conda                | rule       |
      | output               | rule       |
      | log                  | rule       |
      | singularity          | rule       |
      | priority             | rule       |
      | wildcard_constraints | rule       |
      | shell                | rule       |
      | wrapper              | rule       |
      | script               | rule       |
      | cwl                  | rule       |
      | version              | rule       |
      | cache                | rule       |
      | benchmark            | checkpoint |
      | conda                | checkpoint |
      | output               | checkpoint |
      | log                  | checkpoint |
      | singularity          | checkpoint |
      | priority             | checkpoint |
      | wildcard_constraints | checkpoint |
      | shell                | checkpoint |
      | wrapper              | checkpoint |
      | script               | checkpoint |
      | cwl                  | checkpoint |
      | version              | checkpoint |
      | cache                | checkpoint |
