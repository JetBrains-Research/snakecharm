Feature: rule redeclaration gutter
  Scenario Outline: Rule redeclared in same file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME: #1
      input: "name_inp"

    <rule_like> NAME: #2
      input: "name_2_inp"

    use rule FOO as NAME: #3
      input: "name_3_inp"
    """
    When I put the caret at NAME: #1
    Then I expect marker of redeclared in rule with references:
      | NAME | <rule_like> NAME: #2     | foo.smk |
      | NAME | use rule FOO as NAME: #3 | foo.smk |
    When I put the caret at NAME: #2
    Then I expect marker of redeclared in rule with references:
      | NAME | use rule FOO as NAME: #3 | foo.smk |
    When I put the caret at NAME: #3
    Then I expect no markers of redeclared in
    When I put the caret at NAME: #3
    Then I expect marker of it redeclares rule with references:
      | NAME | <rule_like> NAME: #1 | foo.smk |
      | NAME | <rule_like> NAME: #2 | foo.smk |
    When I put the caret at NAME: #2
    Then I expect marker of it redeclares rule with references:
      | NAME | <rule_like> NAME: #1 | foo.smk |
    When I put the caret at NAME: #1
    Then I expect no markers of it redeclares
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

    # TODO: plus other file
    # TODO: plus 2 use rules file

#  Scenario Outline: Rule redeclared in same file but wrong RULE_KEYWORD
#    Given a snakemake project
#    Given I open a file "foo.smk" with text
#    """
#    <rule_like> NAME: #1
#      input: "name_inp"
#
#
#
#    <rule_like> NAME: #2
#      input: "name_2_inp"
#    """
#    # TODO: maybe edit text? insert \n ?