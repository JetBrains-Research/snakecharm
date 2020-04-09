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
         cache: True
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
      | cache                |

  Scenario Outline: Resolve for python specific variables for sections w/o arguments
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      local_var = 1

      <rule_like> NAME:
         <section>: "{<text>}"
      """
    When I put the caret after "{
    Then reference in injection should resolve to "<result>" in "<file>"
    Examples:
      | rule_like  | section | text      | result      | file        |
      | rule       | shell   | config    | config      | workflow.py |
      | rule       | shell   | rules     | rules       | workflow.py |
      | rule       | shell   | local_var | local_var   | foo.smk     |
      | rule       | shell   | input     | InputFiles  | io.py       |
      | rule       | shell   | output    | OutputFiles | io.py       |
      | rule       | shell   | log       | Log         | io.py       |
      | rule       | shell   | params    | Params      | io.py       |
      | rule       | shell   | resources | Resources   | io.py       |
      | rule       | shell   | wildcards | NAME:   | foo.smk       |
      #| rule       | shell   | wildcards | Wildcards   | io.py       |
      | checkpoint | shell   | local_var | local_var   | foo.smk     |
      | rule       | message | config    | config      | workflow.py |
      | checkpoint | message | local_var | local_var   | foo.smk     |

  Scenario Outline: No resolve for python specific methods/classes for sections w/o arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        def local_def():
            pass

        class LocalClass() :
            pass

        <rule_like> NAME:
           <section>: "{<text>}"
        """
    When I put the caret after "{
    Then reference in injection should not resolve
    Examples:
      | rule_like  | section | text       |
      | rule       | shell   | local_def  |
      | rule       | shell   | LocalClass |
      | rule       | shell   | expand     |
      | rule       | shell   | ancient    |
      | rule       | shell   | shell      |
      | checkpoint | shell   | local_def  |
      | checkpoint | shell   | LocalClass |
      | checkpoint | shell   | expand     |
      | checkpoint | shell   | ancient    |
      | checkpoint | shell   | shell      |
      | rule       | message | local_def  |
      | checkpoint | message | shell      |

  Scenario Outline: Resolve for python variables from imported file
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
       <section>: "{<text>}"
    """
    When I put the caret after "{
    Then reference in injection should multi resolve to name, file, times[, class name]
      | <symbol_name> | <file> | <times> |

    Examples:
      | rule_like  | section | text      | symbol_name | file        | times |
      | rule       | shell   | variable1 | variable1   | my_util1.py | 1     |
      | rule       | shell   | variable1 | *           | foo.smk     | 1     |
      | rule       | shell   | CONST1    | CONST1      | my_util1.py | 1     |
      | rule       | shell   | CONST1    | *           | foo.smk     | 1     |
      | rule       | shell   | CONST2    | CONST2      | my_util2.py | 1     |
      | rule       | shell   | CONST2    | CONST2      | foo.smk     | 1     |
      | checkpoint | shell   | variable1 | variable1   | my_util1.py | 1     |
      | checkpoint | shell   | variable1 | *           | foo.smk     | 1     |
      | checkpoint | message | CONST2    | CONST2      | my_util2.py | 1     |
      | checkpoint | message | CONST2    | CONST2      | foo.smk     | 1     |

  Scenario Outline: No resolve after subscription (not supported yet)
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
          input: k = ""
          shell: "{input[k].k}"
      """
      When I put the caret after input[k].
      Then reference in injection should not resolve
      Examples:
        | rule_like  |
        | rule       |
        | checkpoint |