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
      | section         |
      | shell           |
      | threads         |
      | shadow          |

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
        - name: "fooboodoo"
          type: "<keyword>"
      - version: "2.0.0"
        introduced:
        - name: "fooboodoo"
          type: "<keyword>"
          multiple_args_allowed: False
      - version: "0.0.1"
        introduced:
        - name: "fooboodoo"
          type: "top-level"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        fooboodoo: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version | keyword     |
      | 1.0.0        | rule        |
      | 3.0.0        | rule        |
      | 3.0.1        | rule        |
      | 3.0.1        | checkpoint  |
      | 1.0.0        | module      |
      | 3.0.0        | module      |
      | 1.0.0        | subworkflow |
      | 3.0.0        | subworkflow |

  Scenario Outline: Subsections with only one argument when API settings do not allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "fooboodoo"
          type: "<keyword>"
      - version: "2.9.0"
        introduced:
        - name: "fooboodoo"
          type: "top-level"
      - version: "2.0.0"
        introduced:
        - name: "fooboodoo"
          type: "<keyword>"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        fooboodoo: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for 'fooboodoo' section in Snakemake '<lang_version>'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for 'fooboodoo' section in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version | keyword     |
      | 2.0.0        | rule        |
      | 2.10.0       | rule        |
      | 2.10.1       | checkpoint  |
      | 2.0.0        | module      |
      | 2.10.0       | module      |
      | 2.0.0        | subworkflow |
      | 2.10.0       | subworkflow |

  Scenario Outline: workflow sections with only one argument when API settings allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "fooboodoo"
          type: "top-level"
      - version: "2.0.0"
        introduced:
        - name: "fooboodoo"
          type: "top-level"
          multiple_args_allowed: False
      - version: "0.0.1"
        introduced:
        - name: "fooboodoo"
          type: "rule-like"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    fooboodoo: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version |
      | 1.0.0        |
      | 3.0.0        |
      | 3.0.1        |

  Scenario Outline: workflow sections with only one argument when API settings do not allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "fooboodoo"
          type: "top-level"
      - version: "2.9.0"
        introduced:
        - name: "fooboodoo"
          type: "rule-like"
          multiple_args_allowed: False
      - version: "2.0.0"
        introduced:
        - name: "fooboodoo"
          type: "top-level"
          multiple_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    fooboodoo: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for 'fooboodoo' section in Snakemake '<lang_version>'.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for 'fooboodoo' section in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version |
      | 2.0.0        |
      | 2.10.0       |

