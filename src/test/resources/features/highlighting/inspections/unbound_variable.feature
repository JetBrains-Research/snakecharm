Feature: Inspection: Unbound variable Inspection replacement
  Scenario: Works in python code
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    if ccccc:
        tt = 1
    print(tt)
    """
    And PyUnboundLocalVariableInspection inspection is enabled
    And SmkPyUnboundLocalVariableInspection inspection is enabled
    Then I expect inspection warning on <tt> in <print(tt)> with message
    """
    Name 'tt' can be undefined
    """
    When I check highlighting warnings

  Scenario Outline: No error on localrules and ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: NAME
    <rule_like> NAME:
      input: ""
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    And PyUnboundLocalVariableInspection inspection is enabled
    And SmkPyUnboundLocalVariableInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | rule_like  | section    |
      | rule       | localrules |
      | rule       | ruleorder  |
      | checkpoint | localrules |
      | checkpoint | ruleorder  |

