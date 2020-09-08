Feature: Fixes PyArgumentListInspection related false positives
  Issue #319

  Scenario: PyArgumentListInspection  works in snakemake files
      Given a snakemake project
      Given I open a file "foo.smk" with text
      """
      rule rule_319:
          input:
              lambda wildcards: expand(" ", **1)
      """
      And PyArgumentListInspection inspection is enabled
      Then I expect inspection warning on <**1> in <expand(" ", **1)> with message
      """
      Expected a dictionary, got int
      """
      When I check highlighting warnings

  Scenario Outline: Args Section type is iterable
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule_319:
        input:
            lambda wildcards: expand(" ", **wildcards)
    """
    And PyArgumentListInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |