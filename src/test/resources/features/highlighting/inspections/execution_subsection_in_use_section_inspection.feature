Feature: Inspection if subsection is unexpected for section but it i appropriate for another section

  Scenario: When 'use' section contains execution subsections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      use rule RULE as NEW_RULE with:
          run: ""
          shell: ""
          notebook: ""
          input: "file1"
          script: ""
          cwl: ""
          wrapper: ""
          output: "file2"
      """
    And SmkExecutionSubsectionInUseSectionInspection inspection is enabled
    Then I expect inspection error on <run> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    Then I expect inspection error on <shell> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    Then I expect inspection error on <notebook> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    Then I expect inspection error on <script> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    Then I expect inspection error on <cwl> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    Then I expect inspection error on <wrapper> with message
      """
      Execution sections can't be overridden in 'use rule'
      """
    When I check highlighting errors