Feature: Inspection: Unused local Inspection suppressing
  Scenario: Works in python code
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    def foo_385():
        a = 1
    """
    And PyUnusedLocalInspection inspection is enabled
    Then I expect inspection weak warning on <a> with message
    """
    Local variable 'a' value is not used
    """
    When I check highlighting weak warnings

  Scenario Outline: No error on localrules and ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo_385():
        <rule_like>:
            <section>: "a"
        pass
    """
    And PyUnusedLocalInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | shell   |
      | rule       | run     |
      | checkpoint | input   |

