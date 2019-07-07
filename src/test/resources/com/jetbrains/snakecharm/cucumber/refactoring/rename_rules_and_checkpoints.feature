Feature: Rename rules/checkpoints

  Scenario Outline: Rename rule/checkpoint declaration name
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
        output: "out.txt"

      <rule_like> boo:
        input: <rule_like>s.foo
      """
    When I put the caret at <rule_like> foo:
    When I invoke rename with name "doo"
    Then the file "foo.smk" should have text
    """
    <rule_like> doo:
      output: "out.txt"

    <rule_like> boo:
      input: <rule_like>s.doo
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rename rule/checkpoint from usage
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <rule_like> foo:
        output: "out.txt"

      <rule_like> boo:
        input: <rule_like>s.foo
      """
    When I put the caret after <rule_like>s.fo
    When I invoke rename with name "doo"
    Then the file "foo.smk" should have text
    """
    <rule_like> doo:
      output: "out.txt"

    <rule_like> boo:
      input: <rule_like>s.doo
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |