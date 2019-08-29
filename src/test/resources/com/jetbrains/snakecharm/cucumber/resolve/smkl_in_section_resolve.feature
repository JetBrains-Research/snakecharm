Feature: Resolve for sections/variables in SmkSL injections

  Scenario Outline: Resolve to section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        input: ""
        shell: "{input}"
    """
    When I put the caret after {inp
    Then reference in injection should resolve to "input" in "foo.smk"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |


  Scenario Outline: No resolve for unavailable sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
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
         shell: "{<section>}"
         run:
    """
    When I put the caret after "{
    Then reference in injection should not resolve
    Examples:
      | section              |
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

  Scenario Outline: Resolve for available sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
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
         shell: "{<section>}"
         run:
    """
    When I put the caret after "{
    Then reference in injection should resolve to "<section>" in "foo.smk"
    Examples:
      | section              |
      | input                |
      | version              |
      | threads              |
      | output               |
      | log                  |
      | params               |
      | resources            |
