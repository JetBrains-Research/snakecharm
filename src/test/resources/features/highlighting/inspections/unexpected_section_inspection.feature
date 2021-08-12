Feature: Inspection if subsection is unexpected for section but it i appropriate for another section

  Scenario: When 'use' section contains execution subsections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      use rule RULE as NEW_RULE with:
          run: ""
          shell: ""
          notebook: ""
          script: ""
          cwl: ""
          wrapper: ""
      """
    And SmkUnexpectedSectionInspection inspection is enabled
    Then I expect inspection error on <run> with message
      """
      Section 'run' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection error on <shell> with message
      """
      Section 'shell' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection error on <notebook> with message
      """
      Section 'notebook' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection error on <script> with message
      """
      Section 'script' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection error on <cwl> with message
      """
      Section 'cwl' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection error on <wrapper> with message
      """
      Section 'wrapper' can't be used in 'use' but can be in 'rule'
      """
    When I check highlighting errors

  Scenario: Wrong sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      rule NAME:
          skip_validation: True

      checkpoint NAME2:
          meta_wrapper: "wrapper"

      subworkflow NAME3:
          log: "log.txt"

      module m:
          workdir: "dir"
      """
    And SmkUnexpectedSectionInspection inspection is enabled
    Then I expect inspection error on <skip_validation> with message
      """
      Section 'skip_validation' can't be used in 'rule' but can be in 'module'
      """
    Then I expect inspection error on <meta_wrapper> with message
      """
      Section 'meta_wrapper' can't be used in 'checkpoint' but can be in 'module'
      """
    Then I expect inspection error on <log> with message
      """
      Section 'log' can't be used in 'subworkflow' but can be in 'rule'
      """
    Then I expect inspection error on <workdir> with message
      """
      Section 'workdir' can't be used in 'module' but can be in 'subworkflow'
      """
    When I check highlighting errors