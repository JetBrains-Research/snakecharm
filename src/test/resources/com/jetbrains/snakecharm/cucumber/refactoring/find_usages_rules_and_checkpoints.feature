Feature: Find Usages for rules/checkpoints

Scenario Outline: Usages for rule/checkpoint declaration name
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
        output: "out.txt"

      <rule_like> boo:
        input: <rule_like>s.foo

      localrules: foo
      """
    When I put the caret after <rule_like> fo
    And I invoke find usages
    Then find usages shows me following references:
      | file    | offset   | length   |
      | foo.smk | <offset1> | <length1> |
      | foo.smk | <offset2> | <length2> |
    Examples:
      | rule_like  | offset1 | length1 | offset2 | length2 |
      | rule       | 50      | 9       | 73      | 3       |
      | checkpoint | 62      | 15      | 91      | 3       |