Feature: Inspection for multiple arguments in various sections

  Scenario Outline: module/subworkflow sections with only one argument in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    When I check highlighting errors
    Examples:
      | keyword     | section         |
      | subworkflow | configfile      |
      | module      | snakefile       |

  Scenario Outline: rule/checkpoint sections with only one argument in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    When I check highlighting errors
    Examples:
      | section   |
      | shell     |
      | threads   |
      | shadow    |
      | localrule |

  Scenario Outline: Multiple arguments in workflow section in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section_name>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section_name>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section_name>' section in Snakemake 'CURR_SMK_LANG_VERS'.
    """
      When I check highlighting errors
    Examples:
      | section_name  |
      | containerized |
      | singularity   |
      | container     |
      | workdir       |

  Scenario Outline: Subsections with only one argument when API settings allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "<section>"
          type: "<keyword>"

      - version: "2.0.0"
        introduced:
        - name: "<section>"
          type: "<keyword>"
          multiple_args_allowed: False

      - version: "0.0.1"
        introduced:
        - name: "configfile"
          type: "top-level"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version | keyword     | section              |
      | 1.0.0        | rule        | configfile           |
      | 3.0.0        | rule        | wildcard_constraints |
      | 3.0.1        | rule        | fooboodoo            |
      | 3.0.1        | checkpoint  | fooboodoo            |
      | 1.0.0        | module      | configfile           |
      | 3.0.0        | module      | fooboodoo            |
      | 1.0.0        | subworkflow | configfile           |
      | 3.0.0        | subworkflow | fooboodoo            |

  Scenario Outline: Subsections with only one argument when API settings do not allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "<section>"
          type: "<keyword>"

      - version: "2.9.0"
        introduced:
        - name: "configfile"
          type: "top-level"

      - version: "2.0.0"
        introduced:
        - name: "<section>"
          type: "<keyword>"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake '<lang_version>'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version | keyword     | section    |
      | 2.0.0        | rule        | fooboodoo  |
      | 2.10.0       | rule        | configfile |
      | 2.10.1       | checkpoint  | fooboodoo  |
      | 2.0.0        | module      | fooboodoo  |
      | 2.10.0       | module      | configfile |
      | 2.0.0        | subworkflow | fooboodoo  |
      | 2.10.0       | subworkflow | configfile |

  Scenario Outline: workflow sections with only one argument when API settings allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "<section>"
          type: "top-level"

      - version: "2.0.0"
        introduced:
        - name: "<section>"
          type: "top-level"
          multiple_args_allowed: False

      - version: "0.0.1"
        introduced:
        - name: "<section>"
          type: "rule-like"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version | section              |
      | 1.0.0        | configfile           |
      | 3.0.0        | wildcard_constraints |
      | 3.0.1        | wildcard_constraints |

  Scenario Outline: workflow sections with only one argument when API settings do not allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "<section>"
          type: "top-level"
      - version: "2.9.0"
        introduced:
        - name: "<section>"
          type: "rule-like"
          multiple_args_allowed: False
      - version: "2.0.0"
        introduced:
        - name: "<section>"
          type: "top-level"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake '<lang_version>'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version | section              |
      | 2.0.0        | configfile           |
      | 2.10.0       | wildcard_constraints |

