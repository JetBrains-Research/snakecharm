Feature: Inspection if subsection is unexpected for section but it i appropriate for another section

  Scenario Outline: When 'use' section contains execution subsections
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