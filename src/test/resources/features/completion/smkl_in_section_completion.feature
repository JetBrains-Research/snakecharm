Feature: Completion for sections/variables in SmkSL injections

  Scenario Outline: Completion contains supported sections names if defined in rule
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <section>: fa="text"
        <text>
      """
      When I put the caret after "{
      Then I invoke autocompletion popup
      And completion list should contain items <section>

    Examples:
      | rule_like  | section   | text             |
      | rule       | input     | shell: "{}"      |
      | rule       | output    | shell: "{}"      |
      | rule       | resources | shell: "{}"      |
      | rule       | params    | shell: "{}"      |
      | rule       | threads   | shell: "{}"      |
      | rule       | version   | shell: "{}"      |
      | rule       | log       | shell: "{}"      |
      | rule       | input     | run: shell("{}") |
      | rule       | output    | run: shell("{}") |
      | rule       | resources | run: shell("{}") |
      | rule       | params    | run: shell("{}") |
      | rule       | threads   | run: shell("{}") |
      | rule       | version   | run: shell("{}") |
      | rule       | log       | run: shell("{}") |
      | checkpoint | input     | shell: "{}"      |
      | checkpoint | threads   | run: shell("{}") |

  Scenario Outline: Completion list contains supported sections even not available yet (except fake threads, version)
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
           <text>
      """
      When I put the caret after "{
      And I invoke autocompletion popup
      Then completion list should contain:
        | input                |
        | output               |
        | log                  |
        | params               |
        | resources            |
#        | threads              |
#        | version              |

      Examples:
        | rule_like  | text             |
        | rule       | shell: "{}"      |
        | rule       | run: shell("{}") |
        | checkpoint | shell: "{}"      |
        | checkpoint | run: shell("{}") |

  Scenario Outline: Completion list shouldn't contain unsupported sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
         wrapper: ""
         wildcard_constraints: a=""
         conda: ""
         cache: True
         message: ""
         benchmark: ""
         shadow: "full"
         group: ""
         singularity: ""
         cwl: ""
         priority: 1
         script: ""
         shell: "{}"
         run: shell("{}")
    """
    When I put the caret after <signature>
    And I invoke autocompletion popup
    And completion list shouldn't contain:
      | wrapper              |
      | wildcard_constraints |
      | conda                |
      | shell                |
      | message              |
      | benchmark            |
      | shadow               |
      | group                |
      | singularity          |
      | cwl                  |
      | priority             |
      | run                  |
      | script               |
      | cache                |
    Examples:
      | rule_like  | signature     |
      | rule       | shell: "{     |
      | rule       | run: shell("{ |
      | checkpoint | shell: "{     |
      | checkpoint | run: shell("{ |

  Scenario Outline: Complete rule/checkpoint section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
       input: ""
       message: "{i}"
    """
    When I put the caret after {i
    Then I invoke autocompletion popup, select "input" lookup item and see a text:
    """
    <rule_like> NAME:
       input: ""
       message: "{input}"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete python specific variables for sections w/o arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    local_var = 1
    def local_def():
        pass

    class LocalClass() :
        pass

    <rule_like> NAME:
       shell: "{d}"
    """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | config      |
      | rules       |
      | local_var   |
      | wildcards   |
      | rules       |
      | checkpoints |
      | input       |
      | output      |
      | log         |
      | params      |
      | resources   |
      # | threads |
    And completion list shouldn't contain:
      | local_def  |
      | LocalClass |
      | expand     |
      | ancient    |
      | shell      |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete python specific variables from imported file
    Given a snakemake project
    Given a file "my_util1.py" with text
    """
    variable1 = 1
    CONST1 = 11
    """
    Given a file "my_util2.py" with text
    """
    CONST2 = 2
    """
    Given I open a file "foo.smk" with text
    """
    from my_util1 import *
    from my_util2 import CONST2

    <rule_like> NAME:
       <section>: "{}"
    """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | variable1 |
      | CONST1    |
      | CONST2    |
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | checkpoint |  message   |
      | checkpoint |  shell   |

  Scenario Outline: No completion after subscription (not supported yet)
        Given a snakemake project
        Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
            input: k = ""
            shell: "{input[k].k}"
        """
        When I put the caret after input[k].
        And I invoke autocompletion popup
        Then completion list should be empty
        Examples:
          | rule_like  |
          | rule       |
          | checkpoint |