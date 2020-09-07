Feature: Fixes PyArgumentListInspection related false positives
  Scenario Outline: Args Section type is iterable
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> rule_319:
        input:
            lambda wildcards: expand(" ", **wildcards),
            lambda wildcards: expand(" ", **1)
    """
    And PyArgumentListInspection inspection is enabled
    # warning only for `**1`, not for `**wildcards`
    Then I expect inspection warning on <**1> in <expand(" ", **1)> with message
    """
    Expected a dictionary, got int
    """
    When I check highlighting warnings
    Examples:
      | rule_like   |
      | rule        |
      | checkpoint  |