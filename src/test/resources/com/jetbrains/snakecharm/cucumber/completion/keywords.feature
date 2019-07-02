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

  Scenario Outline: Complete at rule level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <str>
    """
    When I put the caret after <str>
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
    """
    rule NAME:
      <result>: 
    """
    Examples:
      | str | result               |
      | inp | input                |
      | out | output               |
      | par | params               |
      | lo  | log                  |
      | re  | resources            |
      | be  | benchmark            |
      | ve  | version              |
      | me  | message              |
      | th  | threads              |
      | si  | singularity          |
      | pr  | priority             |
      | wi  | wildcard_constraints |
      | gr  | group                |
      | sh  | shadow               |
      | co  | conda                |
      | cw  | cwl                  |
      | sc  | script               |
      | sh  | shell                |
      | run | run                  |
      | wr  | wrapper              |

  Scenario: Complete and replace at rule level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "in.txt"
    """
    When I put the caret at input
    Then I invoke autocompletion popup, select "output" lookup item in replace mode and see a text:
    """
    rule NAME:
      output: "in.txt"
    """

