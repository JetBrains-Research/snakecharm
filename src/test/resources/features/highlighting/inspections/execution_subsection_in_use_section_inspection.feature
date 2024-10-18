Feature: Inspection if subsection is unexpected for section but it i appropriate for another section

  Scenario Outline: Show Error when 'use' section contains execution subsections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      use rule RULE as NEW_RULE with:
          input: "file1"
          output: "file2"
          <section>: "foo"
      """
    And SmkExecutionSubsectionInUseSectionInspection inspection is enabled
    Then I expect inspection error on <<section>> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    When I check highlighting errors
    Examples:
      | section         |
      | run             |
      | shell           |
      | notebook        |
      | script          |
      | cwl             |
      | wrapper         |
      | template_engine |

  Scenario Outline: Show Error when 'use' section contains execution subsections configured using YAML
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          execution_section: False

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
          execution_section: True

        - name: "threads"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
      """
      use rule RULE as NEW_RULE with:
          input: "file1"
          output: "file2"
          foobooo: "foo"
      """
    And SmkExecutionSubsectionInUseSectionInspection inspection is enabled
    Then I expect inspection error on <foobooo> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    When I check highlighting errors
    Examples:
      | lang_version |
      | 2.0.0        |

  Scenario Outline: No error when 'use' section contains non-available execution subsections configured using YAML
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          execution_section: False

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
          execution_section: True

        - name: "threads"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
      """
      use rule RULE as NEW_RULE with:
          input: "file1"
          output: "file2"
          foobooo: "foo"
      """
    And SmkExecutionSubsectionInUseSectionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | lang_version |
      | 1.0.0        |
      | 3.0.0        |