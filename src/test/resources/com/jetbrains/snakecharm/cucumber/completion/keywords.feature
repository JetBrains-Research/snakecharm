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
      Then I invoke autocompletion popup, select "<item>" lookup in replace mode and see a text:
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
