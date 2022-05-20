Feature: Completion after rule/checkpoint name e.g. rules.NAME.input

  Scenario Outline: Completion list contains only available sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
         input: ""
         version: 1
         cache: True
         wrapper: ""
         wildcard_constraints: a=""
         conda: ""
         message: ""
         threads: 1
         benchmark: ""
         shadow: "full"
         output: ""
         group: ""
         singularity: ""
         cwl: ""
         log: ""
         params: a=""
         priority: 1
         resources: a=""
         envmodules: "foo"
         default_target: True
         script: ""
         shell: ""
         name: "new_rule_name"
         run:

     <rule_like> ANOTHER_NAME:
        message: <injection_left><rule_like>s.NAME.<injection_right>
    """
    When I put the caret after <rule_like>s.NAME.
    And I invoke autocompletion popup
    Then completion list should contain:
      | input                |
      | version              |
      | wrapper              |
      | wildcard_constraints |
      | message              |
      | benchmark            |
      | output               |
      | log                  |
      | params               |
      | priority             |
      | resources            |
    And completion list shouldn't contain:
      | conda          |
      | envmodules     |
      | shell          |
      | threads        |
      | shadow         |
      | group          |
      | singularity    |
      | cwl            |
      | run            |
      | script         |
      | name           |
      | handover       |
      | default_target |
    Examples:
      | rule_like  | injection_left | injection_right |
      | rule       |                |                 |
      | checkpoint |                |                 |
      | rule       | "{             | }"              |
      | checkpoint | "{             | }"              |

  Scenario Outline: Completion for sections at top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""

    <rule_like>s.NAME.i
    """
    When I put the caret after <rule_like>s.NAME.i
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
        input: ""

    <rule_like>s.NAME.input
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion list doesn't contain undeclared sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""
        message: ""

    <rule_like>s.NAME.
    """
    When I put the caret after <rule_like>s.NAME.
    And I invoke autocompletion popup
    Then completion list should contain:
    | input  |
    And completion list shouldn't contain:
      | version              |
      | wrapper              |
      | wildcard_constraints |
      | benchmark            |
      | output               |
      | log                  |
      | params               |
      | priority             |
      | resources            |
      | conda                |
      | shell                |
      | threads              |
      | shadow               |
      | group                |
      | envmodules           |
      | singularity          |
      | cwl                  |
      | run                  |
      | script               |
      | name                 |
      | handover             |
      | default_target       |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion for section's keyword arguments in injection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: kwd1="arg1", kwd2="arg2"
      shell: "{<rule_like>s.NAME.<section>.}"
    """
    When I put the caret after NAME.<section>.
    And I invoke autocompletion popup
    Then completion list should contain:
      | kwd1 |
      | kwd2 |
    Examples:
      | section   | rule_like  |
      | input     | rule       |
      | output    | rule       |
      | resources | rule       |
      | log       | rule       |
      | input     | checkpoint |
      | output    | checkpoint |
      | resources | checkpoint |
      | log       | checkpoint |

  Scenario Outline: Completion for section's keyword arguments for rules in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: kwd1="arg1", kwd2="arg2"

    <rule_like> ANOTHER_NAME:
      input: <rule_like>s.NAME.<section>.
    """
    When I put the caret after NAME.<section>.
    And I invoke autocompletion popup
    Then completion list should contain:
      | kwd1 |
      | kwd2 |
    Examples:
      | section   | rule_like  |
      | input     | rule       |
      | output    | rule       |
      | resources | rule       |
      | log       | rule       |
      | input     | checkpoint |
      | output    | checkpoint |
      | resources | checkpoint |
      | log       | checkpoint |

  Scenario Outline: Completion for section's keyword arguments for rules in top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: kwd1="arg1", kwd2="arg2"

    <rule_like>s.NAME.<section>.
    """
    When I put the caret after NAME.<section>.
    And I invoke autocompletion popup
    Then completion list should contain:
      | kwd1 |
      | kwd2 |
    Examples:
      | section   | rule_like  |
      | input     | rule       |
      | output    | rule       |
      | resources | rule       |
      | log       | rule       |
      | input     | checkpoint |
      | output    | checkpoint |
      | resources | checkpoint |
      | log       | checkpoint |

  Scenario Outline: Complete rule/checkpoint section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""

    <rule_like> ANOTHER_NAME:
        message: <injection_left><rule_like>s.NAME.i<injection_right>
    """
    When I put the caret after <rule_like>s.NAME.i
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
        input: ""

    <rule_like> ANOTHER_NAME:
        message: <injection_left><rule_like>s.NAME.input<injection_right>
    """
    Examples:
      | rule_like  | injection_left | injection_right |
      | rule       |                |                 |
      | checkpoint |                |                 |
      | rule       | "{             | }"              |
      | checkpoint | "{             | }"              |

  Scenario Outline: Completion for section's keyword arguments in subscription expression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: kwd1="arg1", kwd2="arg2"
      shell: "{<rule_like>s.NAME.<section>[]}"
    """
    When I put the caret after NAME.<section>[
    And I invoke autocompletion popup
    Then completion list should contain:
      | kwd1 |
      | kwd2 |
    Examples:
      | section   | rule_like  |
      | input     | rule       |
      | output    | rule       |
      | resources | rule       |
      | log       | rule       |
      | input     | checkpoint |
      | output    | checkpoint |
      | resources | checkpoint |
      | log       | checkpoint |

