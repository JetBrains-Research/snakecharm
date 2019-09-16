Feature: Goto feature test

  Scenario Outline: Go to rule/checkpoint name
    Given a snakemake project
    Given a file "foo1.smk" with text
    """
    <rule_like> name11:
      input: "path/to/input"

    <rule_like> name12:
      input: "path/to/input"
    """
    And a file "foo2.smk" with text
    """
    <rule_like> name21:
      input: "path/to/input"

    <rule_like> name22:
      input: "path/to/input"
    """
    Then go to symbol should contain:
    | name11 |
    | name12 |
    | name21 |
    | name22 |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |