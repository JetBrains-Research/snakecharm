Feature: Inspection for unexpected keyword arguments in section

  Scenario Outline: Unexpected keyword arguments in subworkflow in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section>' does not support keyword arguments in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    When I check highlighting errors
    Examples:
      | section    |
      | workdir    |
      | snakefile  |
      | configfile |

  Scenario Outline: Unexpected keyword arguments in rule\checkpoint\module in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section>' does not support keyword arguments in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    When I check highlighting errors
    Examples:
      | rule_like  | section         |
      | rule       | benchmark       |
      | rule       | cache           |
      | rule       | conda           |
      | rule       | container       |
      | rule       | containerized   |
      | rule       | cwl             |
      | rule       | group           |
      | rule       | envmodules      |
      | rule       | singularity     |
      | rule       | threads         |
      | rule       | name            |
      | rule       | handover        |
      | rule       | default_target  |
      | rule       | retries         |
      | rule       | template_engine |
      | checkpoint | message         |
      | checkpoint | notebook        |
      | checkpoint | priority        |
      | checkpoint | script          |
      | checkpoint | shadow          |
      | checkpoint | shell           |
      | checkpoint | version         |
      | checkpoint | wrapper         |
      | checkpoint | handover        |
      | module     | snakefile       |
      | module     | config          |
      | module     | skip_validation |
      | module     | meta_wrapper    |


  Scenario Outline: No warn on expected keyword arguments in rule\checkpoint in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section              |
      | rule       | input                |
      | rule       | output               |
      | rule       | params               |
      | checkpoint | log                  |
      | checkpoint | resources            |
      | checkpoint | wildcard_constraints |

  Scenario Outline: Unexpected keyword arguments on top-level in latest language level
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section_name>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section_name>' does not support keyword arguments in Snakemake 'CURR_SMK_LANG_VERS'.
    """
    When I check highlighting errors
    Examples:
      | section_name  |
      | containerized |
      | singularity   |
      | container     |

  Scenario Outline: No warn on expected keyword arguments in subsections when API settings allow
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
          keyword_args_allowed: False
      - version: "0.0.1"
        introduced:
        - name: "fooboodoo"
          type: "top-level"
          keyword_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        fooboodoo: a="foo.bar", b="boo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
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

  Scenario Outline: Unexpected keyword arguments in subsections when API settings do not allow
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
          keyword_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        fooboodoo: a="foo.bar", b="boo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section 'fooboodoo' does not support keyword arguments in Snakemake '<lang_version>'.
    """
    Then I expect inspection error on <b="boo.bar"> with message
    """
    Section 'fooboodoo' does not support keyword arguments in Snakemake '<lang_version>'.
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
  
  Scenario Outline: No warn on expected keyword arguments on top-level when API settings allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "configfile"
          type: "top-level"
      - version: "2.0.0"
        introduced:
        - name: "configfile"
          type: "top-level"
          keyword_args_allowed: False
      - version: "0.0.1"
        introduced:
        - name: "configfile"
          type: "rule-like"
          keyword_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    configfile: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version |
      | 1.0.0        |
      | 3.0.0        |
      | 3.0.1        |

  Scenario Outline: Unexpected keyword arguments on top-level when API settings do not allow
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "configfile"
          type: "top-level"
      - version: "2.9.0"
        introduced:
        - name: "configfile"
          type: "rule-like"
          keyword_args_allowed: False
      - version: "2.0.0"
        introduced:
        - name: "configfile"
          type: "top-level"
          keyword_args_allowed: False
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    configfile: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section 'configfile' does not support keyword arguments in Snakemake '<lang_version>'.
    """
    When I check highlighting errors
    Examples:
      | lang_version |
      | 2.0.0        |
      | 2.10.0       |