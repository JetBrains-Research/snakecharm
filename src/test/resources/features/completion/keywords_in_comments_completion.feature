Feature: Completion in comment
  Scenario Outline: Completion for commented sections at top level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    # <prefix>
    """
    When I put the caret after <prefix>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <full_name> |
    And completion list shouldn't contain:
      | <wrong_name> |
    Examples:
      | prefix | full_name            | wrong_name |
      | c      | checkpoint           | conda      |
      | c      | configfile           | conda      |
      | r      | rule                 | resources  |
      | r      | ruleorder            | resources  |
      | s      | subworkflow          | shell      |
      | s      | singularity          | shell      |
      | i      | include              | input      |
      | o      | onsuccess            | output     |
      | o      | onstart              | output     |
      | o      | onerror              | output     |
      | l      | localrules           | log        |
      | w      | workdir              | cwl        |
      | w      | wildcard_constraints | cwl        |

  Scenario Outline: Completion for commented rule/checkpoint sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME: # <prefix>
    """
    When I put the caret after # <prefix>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <full_name> |
    And completion list shouldn't contain:
      | <wrong_name> |
    Examples:
      | rule_like   | prefix | full_name  | wrong_name           |
      | rule        | c      | conda      | checkpoint           |
      | rule        | c      | conda      | configfile           |
      | rule        | r      | resources  | ruleorder            |
      | rule        | s      | shell      | subworkflow          |
      | rule        | i      | input      | include              |
      | checkpoint  | o      | output     | onsuccess            |
      | checkpoint  | o      | output     | onstart              |
      | checkpoint  | o      | output     | onerror              |
      | checkpoint  | l      | log        | localrules           |
      | checkpoint  | w      | wrapper    | workdir              |

  Scenario Outline: Completion for commented subworkflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME: # <prefix>#
    """
    When I put the caret after # <prefix>
    Then I invoke autocompletion popup and see a text:
    """
    subworkflow NAME: # <full_name> #
    """
    Examples:
      | prefix | full_name   |
      | workd  | workdir:    |
      | confi  | configfile: |
      | snake  | snakefile:  |

  Scenario Outline: No completion for commented arguments section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
         <section>: "1", # <prefix>
    """
    When I put the caret after # <prefix>
    And I invoke autocompletion popup
    Then completion list should be empty
    Examples:
      | rule_like   | section       | prefix |
      | rule        | input         | c      |
      | rule        | output        | c      |
      | rule        | log           | i      |
      | rule        | conda         | s      |
      | rule        | shell         | i      |
      | checkpoint  | input         | w      |
      | checkpoint  | output        | l      |
      | checkpoint  | log           | r      |
      | checkpoint  | conda         | l      |
      | checkpoint  | shell         | w      |
      | subworkflow | workdir       | t      |
      | subworkflow | configfile    | d      |
      | subworkflow | snakemakefile | c      |
