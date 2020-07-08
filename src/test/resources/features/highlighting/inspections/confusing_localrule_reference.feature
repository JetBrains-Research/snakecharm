Feature: Inspection warns about confusing localrules names.

  Scenario Outline: Confusing ref
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> boo:
      input: "in"
    """
    And I open a file "foo.smk" with text
    """
    localrules: foo2, boo, foo1
    rule foo1:
      input: "in"

    checkpoint foo2:
      input: "in"
    """
    When SmkLocalRuleConfusingReference inspection is enabled
    Then I expect inspection weak warning on <boo> with message
    """
    Rule 'boo' isn't defined in this file, not an error but it is confusing.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: No confusing ref when overridden
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like1> boo:
      input: "in"
    """
    Given I open a file "foo.smk" with text
    """
    localrules: boo
    <rule_like2> boo:
      input: "in"
    """
    When SmkLocalRuleConfusingReference inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like1 | rule_like2 |
      | rule       | rule       |
      | rule       | checkpoint |
      | checkpoint | rule       |
      | checkpoint | checkpoint |