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

  Scenario: Complete at rule level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      output: "out.txt",
      input: "in.txt"
    """
    When I put the caret at input
    And I invoke autocompletion popup
    Then completion list should contain:
       | input  |
       | output |
       | run    |

  Scenario: Complete at rule section level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      output: "out.txt"
      input: "in.txt"
    """
    When I put the caret at "in
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
       | output |
       | run    |

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


  Scenario Outline: Complete at checkpoint level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
      <str>
    """
    When I put the caret after <str>
    Then I invoke autocompletion popup, select "<result>" lookup item and see a text:
    """
    checkpoint NAME:
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

  Scenario: Complete and replace at checkpoint level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
      input: "in.txt"
    """
    When I put the caret at input
    Then I invoke autocompletion popup, select "output" lookup item in replace mode and see a text:
    """
    checkpoint NAME:
      output: "in.txt"
    """

  Scenario: Complete at checkpoint level after comma
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
      output: "out.txt",
      input: "in.txt"
    """
    When I put the caret at input
    And I invoke autocompletion popup
    Then completion list should contain:
      | input  |
      | output |
      | run    |

  Scenario: Complete at checkpoint section level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    checkpoint NAME:
      output: "out.txt"
      input: "in.txt"
    """
    When I put the caret at "in
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | output     |
      | run        |
      | checkpoint |
