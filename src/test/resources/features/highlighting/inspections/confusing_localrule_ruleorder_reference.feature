Feature: Inspection warns about confusing localrules or ruleorder names.

  Scenario Outline: Confusing localrule/ruleorder ref
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> boo1:
      input: "in"

    use rule boo1 as boo2 with:
      input: "in_2"
    """
    And I open a file "foo.smk" with text
    """
    <section>: foo2<separator> <name><separator> foo1
    rule foo1:
      input: "in"

    checkpoint foo2:
      input: "in"
    """
    When SmkLocalrulesRuleorderConfusingReference inspection is enabled
    Then I expect inspection weak warning on <<name>> with message
    """
    Rule '<name>' isn't defined in this file, not an error but it is confusing.
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  | section    | separator | name |
      | rule       | localrules | ,         | boo1 |
      | checkpoint | localrules | ,         | boo2 |
      | rule       | ruleorder  | >         | boo1 |
      | checkpoint | ruleorder  | >         | boo2 |

  Scenario Outline: No confusing localrule/ruleorder ref when overridden
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like1> boo:
      input: "in"
    """
    Given I open a file "foo.smk" with text
    """
    <section>: boo
    <rule_like2> boo:
      input: "in"
    """
    When SmkLocalrulesRuleorderConfusingReference inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | rule_like1 | rule_like2 |  section  |
      | rule       | rule       | localrule |
      | rule       | checkpoint | localrule |
      | checkpoint | rule       | localrule |
      | checkpoint | checkpoint | localrule |
      | rule       | rule       | ruleorder |
      | rule       | checkpoint | ruleorder |
      | checkpoint | rule       | ruleorder |
      | checkpoint | checkpoint | ruleorder |

  Scenario Outline: No confusing localrule/ruleorder ref for 'use rule'
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: boo2

    rule boo:
      input: "in"

    use rule boo as boo2 with:
      input: "in_2"
    """
    When SmkLocalrulesRuleorderConfusingReference inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | section   |
      | localrule |
      | ruleorder |