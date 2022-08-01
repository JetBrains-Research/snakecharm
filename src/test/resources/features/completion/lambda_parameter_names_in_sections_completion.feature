Feature: Completion for lambda parameter names in specific sections

  Scenario Outline: Completion wildcards in all sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: <lambda> w #
    """
    When I put the caret after lambda w
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> rule1:
      <section>: <lambda> <inserted>
    """
    Examples:
      | rule_like  | section   | lambda   | inserted      |
      | rule       | input     | lambda   | wildcards:  # |
      | rule       | group     | lambda   | wildcards:  # |
      | rule       | params    | p=lambda | wildcards #   |
      | rule       | resources | p=lambda | wildcards #   |
      | rule       | conda     | lambda   | wildcards #   |
      | checkpoint | input     | lambda   | wildcards:  # |
      | checkpoint | params    | p=lambda | wildcards #   |

  Scenario Outline: Completion in section with tail text for the last parameter
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> rule1:
        <section>: <lambda>, #
      """
    When I put the caret at #
    And I invoke autocompletion popup, select "<option>" lookup item and see a text:
      """
      <rule_like> rule1:
        <section>: <lambda>, <option>: #
      """
    Examples:
      | rule_like  | section   | lambda                                     | option  |
      | rule       | params    | lambda wildcards, input, output, resources | threads |
      | rule       | threads   | lambda wildcards, input                    | attempt |
      | rule       | resources | a=lambda wildcards, input, threads         | attempt |
      | rule       | conda     | a=lambda wildcards, params                 | input   |
      | checkpoint | params    | lambda wildcards, input, output, resources | threads |

  Scenario Outline: Completion in conda section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      conda: lambda # purely for this test because I can't insert space into a step definition
    """
    When I put the caret at #
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards |
      | params    |
      | input     |
    And completion list shouldn't contain:
      | output    |
      | resources |
      | threads   |
      | attempts  |
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

  Scenario Outline: Completion in conda section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      conda: a=lambda # purely for this test because I can't insert space into a step definition
    """
    When I put the caret at #
    And I invoke autocompletion popup
    Then completion list should contain:
      | wildcards |
      | params    |
      | input     |
    And completion list shouldn't contain:
      | output    |
      | threads   |
      | attempt   |
      | resources |
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
      | params    |
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

  Scenario Outline: No completion with tail text for first parameters in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule1:
      <section>: <lambda>,
    """
    When I put the caret after ,
    And I invoke autocompletion popup, select "<option>" lookup item and see a text:
    """
    <rule_like> rule1:
      <section>: <lambda>,<option>
    """
    Examples:
      | rule_like  | section   | lambda           | option |
      | rule       | params    | lambda wildcards | input  |
      | rule       | threads   | lambda wildcards | input  |
      | rule       | resources | lambda wildcards | input  |
      | rule       | conda     | lambda wildcards | params |
      | checkpoint | params    | lambda wildcards | input  |
