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

  #  Doesn't work:
#    Scenario Outline: When rule like section isn't recognized by SnakeCharm and we mark it as ignored
#      Given a snakemake project
#      Given I open a file "foo.smk" with text
#      """
#      <rule_like>:
#          unknown_section: ""
#      """
#      And SmkUnrecognizedSectionInspection inspection is enabled
#      Then I expect inspection weak warning on <unknown_section> with message
#      """
#      Section 'unknown_section' isn't recognized by SnakeCharm plugin or there could be a typo in the section name.
#      """
#      When I check highlighting weak warnings
#      Then I invoke quick fix Ignore an unrecognized section 'unknown_section' and see text:
#      """
#      <rule_like>:
#          unknown_section: ""
#      """
#      And I expect no inspection weak warnings
#      Examples:
#        | rule_like   |
#        | rule        |
#        | subworkflow |
#        | checkpoint  |