Feature: Completion for sections/variables in SmkSL injections

  Scenario Outline: Completion list contains only available sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
         input: ""
         version: 1
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
         script: ""
         shell: "{}"
         run:
    """
    When I put the caret after "{
    And I invoke autocompletion popup
    Then completion list should contain:
      | input                |
      | version              |
      | threads              |
      | output               |
      | log                  |
      | params               |
      | resources            |
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
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Completion for sections without arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: fa="text"
      threads: 4
      shell: "command {}"
    """
    When I put the caret after command {
    And I invoke autocompletion popup
    Then completion list should contain:
      | input   |
      | threads |
    And completion list shouldn't contain:
      | output               |
      | version              |
      | log                  |
      | resources            |
      | params               |
      | shadow               |
      | group                |
      | singularity          |
      | wildcard_constraints |
      | conda                |
      | benchmark            |
      | message              |
      | priority             |
      | run                  |
      | script               |
      | shell                |
      | wrapper              |
      | cwl                  |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: completion for incomplete section names in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: fa="text"
      run:
        shell("{<substring>}")
    """
    When I put the caret after "{<substring>
    Then I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      <section>: fa="text"
      run:
        shell("{<section>}")
    """
    Examples:
      | rule_like  | section   | substring |
      | rule       | input     | inp       |
      | rule       | output    | out       |
      | rule       | resources | res       |
      | rule       | params    | par       |
      | rule       | threads   | thr       |
      | rule       | version   | ver       |
      | rule       | log       | lo        |
      | checkpoint | input     | inp       |
      | checkpoint | output    | out       |
      | checkpoint | resources | res       |
      | checkpoint | params    | par       |
      | checkpoint | threads   | thr       |
      | checkpoint | version   | ver       |
      | checkpoint | log       | lo        |

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
