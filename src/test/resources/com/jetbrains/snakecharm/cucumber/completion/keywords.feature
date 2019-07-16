Feature: Completion for snakemake keyword-like things

  Scenario Outline: Complete at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
    """
    <item>: "foo"
    """
    Examples:
    | item                  |
    | configfile            |
    | singularity           |
    | include               |
    | workdir               |
    | wildcard_constraints  |
    | onsuccess             |
    | onstart               |
    | onerror               |
    | localrules            |
    | ruleorder             |

  Scenario: Complete report at top-level
    # We have 2 'report' in completion list, so prev scenario outline doesn't work  for 'report'
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    And I invoke autocompletion popup
    Then completion list should contain:
    | report |

  Scenario Outline: Complete rule-like at top-level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    "foo"
    """
    When I put the caret at "foo"
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
    """
    <item> "foo"
    """
    Examples:
    | item         |
    | rule         |
    | checkpoint   |

  Scenario Outline: Replace at toplevel
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      roo: "foo"
      """
      When I put the caret at roo:
      Then I invoke autocompletion popup, select "<item>" lookup item in replace mode and see a text:
      """
      <item>: "foo"
      """
      Examples:
      | item                  |
      | configfile            |
      | singularity           |
      | include               |
      | workdir               |
      | wildcard_constraints  |
      | onsuccess             |
      | onstart               |
      | onerror               |
      | localrules            |
      | ruleorder             |

  Scenario: No smk keywords in py file
      Given a snakemake project
      Given I open a file "foo.py" with text
      """
      foo = 1;
      """
      When I put the caret after foo = 1;
      And I invoke autocompletion popup
      Then completion list shouldn't contain:
      | configfile            |
      | singularity           |
      | include               |
      | workdir               |
      | wildcard_constraints  |
      | onsuccess             |
      | onstart               |
      | onerror               |
      | localrules            |
      | ruleorder             |
      | rule                  |
      | checkpoint            |

  Scenario Outline: Complete at rule/checkpoint level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt",
      <text>
    """
    When I put the caret at <text>
    And I invoke autocompletion popup
    Then completion list should contain:
       | input  |
       | output |
       | run    |
    Examples:
      | rule_like | text            |
      | rule      | # here          |
      | rule      | input: "in.txt" |
#      | checkpoint | # here          |  TODO[for darya] uncomment when checkpoint completion is done
#      | checkpoint | input: "in.txt" |  TODO[for darya] uncomment when checkpoint completion is done

  Scenario Outline: Complete at rule/checkpoint level after comma (args on new line)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output:
         "out.txt",
      <text>
    """
    When I put the caret at <text>
    And I invoke autocompletion popup
    Then completion list should contain:
      | input  |
      | output |
      | run    |
    Examples:
      | rule_like  | text            |
      | rule       | # here          |
      | rule       | input: "in.txt" |
#      | checkpoint | # here          | TODO[for darya] uncomment when checkpoint completion is done
#      | checkpoint | input: "in.txt" | TODO[for darya] uncomment when checkpoint completion is done

  Scenario: Complete and replace at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
      configfile: "in.txt"
    """
    When I put the caret at configfile
    Then I invoke autocompletion popup, select "snakefile" lookup item in replace mode and see a text:
    """
    subworkflow NAME:
      snakefile: "in.txt"
    """

  Scenario Outline: Complete at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
      <str>#here
    """
    When I put the caret at #here
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
    """
    subworkflow NAME:
      <result>: #here
    """
    Examples:
      | str | result               |
      | con | configfile           |
      | sna | snakefile            |
      | wor | workdir              |

  Scenario: Completion list at subworkflow level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow a:
    """
    When I put the caret after subworkflow a:
    And I invoke autocompletion popup
    Then completion list should contain:
      | configfile |
      | snakefile  |
      | workdir    |
    And completion list shouldn't contain:
      | subworkflow |
      | rule        |

  Scenario Outline: Complete at rule/checkpoint level
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> NAME:
        <str>
      """
    When I put the caret after <str>
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
      """
      <rule_like> NAME:
        <result>: 
      """
    Examples:
      | rule_like  | str | result               |
      | rule       | inp | input                |
      | checkpoint | inp | input                |
      | rule       | out | output               |
      | checkpoint | out | output               |
      | rule       | par | params               |
      | checkpoint | par | params               |
      | rule       | lo  | log                  |
      | checkpoint | lo  | log                  |
      | rule       | re  | resources            |
      | checkpoint | re  | resources            |
      | rule       | be  | benchmark            |
      | checkpoint | be  | benchmark            |
      | rule       | ve  | version              |
      | checkpoint | ve  | version              |
      | rule       | me  | message              |
      | checkpoint | me  | message              |
      | rule       | th  | threads              |
      | checkpoint | th  | threads              |
      | rule       | si  | singularity          |
      | checkpoint | si  | singularity          |
      | rule       | pr  | priority             |
      | checkpoint | pr  | priority             |
      | rule       | wi  | wildcard_constraints |
      | checkpoint | wi  | wildcard_constraints |
      | rule       | gr  | group                |
      | checkpoint | gr  | group                |
      | rule       | sh  | shadow               |
      | checkpoint | sh  | shadow               |
      | rule       | co  | conda                |
      | checkpoint | co  | conda                |
      | rule       | cw  | cwl                  |
      | checkpoint | cw  | cwl                  |
      | rule       | sc  | script               |
      | checkpoint | sc  | script               |
      | rule       | sh  | shell                |
      | checkpoint | sh  | shell                |
      | rule       | run | run                  |
      | checkpoint | run | run                  |
      | rule       | wr  | wrapper              |
      | checkpoint | wr  | wrapper              |

  Scenario Outline: Complete and replace at rule/checkpoint level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "in.txt"
    """
    When I put the caret at input
    Then I invoke autocompletion popup, select "output" lookup item in replace mode and see a text:
    """
    <rule_like> NAME:
      output: "in.txt"
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at rule/checkpoint level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt",
      input: "in.txt"
    """
    When I put the caret at input
    And I invoke autocompletion popup
    Then completion list should contain:
      | input  |
      | output |
      | run    |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete at rule/checkpoint section level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "out.txt"
      input: "in.txt"
    """
    When I put the caret at "in
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | output     |
      | run        |
      | checkpoint |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Do not show rule/checkpoint section keywords where not needed
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> all:
        <text>
      """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | message    |
      | run        |
    Examples:
      | rule_like  | text              | ptn   |
      | rule       | input: foo        | foo   |
      | rule       | input: foo, #here | #here |
      | rule       | input: foo.boo    | boo   |
      | rule       | run: foo          | foo   |
      | checkpoint | input: foo        | foo   |
      | checkpoint | input: foo, #here | #here |
      | checkpoint | run: foo          | foo   |

  Scenario Outline: Do not show subworkflow section keywords where not needed
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      subworkflow all:
        snakefile: <text>
      """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | snakefile  |
      | configfile |
    Examples:
      | text       | ptn   |
      | foo        | foo   |
      | foo, #here | #here |
      | foo.boo    | boo   |

  Scenario: Do not show toplevel keywords in dot reference #97 completion
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rules.
    """
    When I put the caret after rules.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | configfile |
      | workdir    |
      | onstart    |

  Scenario Outline: Do not show toplevel keywords in rule like declarations
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <rule_like> all:
       <text>
     """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
    Examples:
      | rule_like   | text                | ptn   |
      | rule        | input: foo          | foo   |
      | rule        | input: foo, #here   | #here |
      | rule        | input: foo.boo      | boo   |
      | rule        | run: foo            | foo   |
      | checkpoint  | input: foo          | foo   |
      | checkpoint  | input: foo, #here   | #here |
      | checkpoint  | input: foo.boo      | boo   |
      | checkpoint  | run: foo            | foo   |
      | subworkflow | workdir: foo        | foo   |
      | subworkflow | workdir: foo, #here | #here |
      | subworkflow | workdir: foo.boo    | boo   |

  Scenario Outline: Do not show toplevel keywords in workflow sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section>: #here
     """
    When I put the caret at #here
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
      | ruleorder  |
    Examples:
      | section    |
      | include    |
      | configfile |

  Scenario Outline: Do not show toplevel keywords in python expressions
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <text>
     """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | localrules |
      | onstart    |
    Examples:
      | text         | ptn     |
      | "" #here     | " #here |
      | import #here | #here   |
      | from #here   | #here   |

  Scenario Outline: Complete toplevel keywords in python blocks
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      if True:
        # here 1
      elif True:
        # here 2
      else:
        # here 3

      try:
        # here 4
      except:
        # here 5
      """
    When I put the caret at # here <ptn>
    And I invoke autocompletion popup
    Then completion list should contain:
      | localrules |
      | onstart    |
      | rule       |
      | checkpoint |
    Examples:
      | ptn |
      | 1   |
      | 2   |
      | 3   |
      | 4   |
      | 5   |