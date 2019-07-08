Feature: Find Usages for rules/checkpoints

Scenario Outline: Usages for rule/checkpoint declaration name
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
        output: "out.txt"

      <rule_like> boo:
        input: <rule_like>s.foo
      """
    When I put the caret after <rule_like> fo
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset   | length   |
      | foo.smk | <offset> | <length> |
    Examples:
      | rule_like  | offset | length |
      | rule       | 50     | 9      |
      | checkpoint | 62     | 15     |