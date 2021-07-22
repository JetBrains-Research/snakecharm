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
      """
      And SmkUnrecognizedSectionInspection inspection is enabled
      Then I expect inspection weak warning on <run> with message
      """
      Section 'run' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
      Then I expect inspection weak warning on <shell> with message
      """
      Section 'shell' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
      Then I expect inspection weak warning on <notebook> with message
      """
      Section 'notebook' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
      Then I expect inspection weak warning on <script> with message
      """
      Section 'script' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
      Then I expect inspection weak warning on <cwl> with message
      """
      Section 'cwl' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
      """
      When I check highlighting weak warnings