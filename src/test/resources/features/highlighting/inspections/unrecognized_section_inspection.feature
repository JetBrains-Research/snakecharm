Feature: Inspection if section isn't recognized by SnakeCharm

  # TODO: provide some exceptions list for user - e.g. keep it in settings / using quick fix

  Scenario Outline: When rule like section isn't recognized by SnakeCharm
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          unrecognized_section: ""
      """
    And SmkUnrecognizedSectionInspection inspection is enabled
    Then I expect inspection weak warning on <unrecognized_section> with message
      """
      Section 'unrecognized_section' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
    When I check highlighting weak warnings
    Then I see available quick fix: Ignore an unrecognized section 'unrecognized_section'
    Examples:
      | rule_like   |
      | rule        |
      | subworkflow |
      | checkpoint  |

    Scenario Outline: When quick fix is, applied check no warning
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      <rule_like>:
          unknown_section: ""
      """
      And SmkUnrecognizedSectionInspection inspection is enabled
      And I emulate quick fix apply: ignore unresolved item 'unknown_section'
      Then I expect no inspection weak warnings
      When I check highlighting weak warnings
      Examples:
        | rule_like   |
        | rule        |
        | subworkflow |
        | checkpoint  |

  Scenario Outline: When quick fix is applied, check such element in ignored list
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like>:
          unknown_section: ""
      """
    And SmkUnrecognizedSectionInspection inspection is enabled
    Then I expect inspection weak warning on <unknown_section> with message
      """
      Section 'unknown_section' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
    When I check highlighting weak warnings
    And I emulate quick fix apply: ignore unresolved item 'unknown_section'
    Then I check ignored element <unknown_section>
    Examples:
      | rule_like   |
      | rule        |
      | subworkflow |
      | checkpoint  |

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
          unknown_section: ""
      """
    And SmkUnrecognizedSectionInspection inspection is enabled
    Then I expect inspection weak warning on <run> with message
      """
      Section 'run' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <shell> with message
      """
      Section 'shell' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <notebook> with message
      """
      Section 'notebook' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <script> with message
      """
      Section 'script' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <cwl> with message
      """
      Section 'cwl' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <wrapper> with message
      """
      Section 'wrapper' can't be used in 'use' but can be in 'rule'
      """
    Then I expect inspection weak warning on <unknown_section> with message
      """
      Section 'unknown_section' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
    When I check highlighting weak warnings

  Scenario: Wrong sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      rule NAME:
          skip_validation: True

      subworkflow NAME2:
          log: "log.txt"

      module m:
          workdir: "dir"
      """
    And SmkUnrecognizedSectionInspection inspection is enabled
    Then I expect inspection weak warning on <skip_validation> with message
      """
      Section 'skip_validation' can't be used in 'rule' or 'checkpoint' but can be in 'module'
      """
    Then I expect inspection weak warning on <log> with message
      """
      Section 'log' can't be used in 'subworkflow' but can be in 'rule'
      """
    Then I expect inspection weak warning on <workdir> with message
      """
      Section 'workdir' can't be used in 'module' but can be in 'subworkflow'
      """
    When I check highlighting weak warnings