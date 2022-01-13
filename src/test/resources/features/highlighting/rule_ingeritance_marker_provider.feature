Feature: tests line marker provider in case of rule inheritance

  Scenario Outline: Rule inheritance in the one file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      input: "name_inp"

    <rule_like> NAME_2:
      input: "name_2_inp"

    use rule NAME as NAME_3 with:
      threads: 1

    use rule NAME as NAME_4 with:
      threads: 2
    """
    When I put the caret at NAME:
    Then I expect marker of overridden section with references:
      | NAME_3 | foo.smk |
      | NAME_4 | foo.smk |
    When I put the caret at NAME_2:
    Then I expect no markers
    When I put the caret at NAME_3
    Then I expect marker of overriding section with references:
      | NAME | foo.smk |
    When I put the caret at NAME_4
    Then I expect marker of overriding section with references:
      | NAME | foo.smk |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Rule inheritance in case of module usage
    Given a snakemake project
    And a file "boo.smk" with text
    """
    <rule_like> NAME:
      input: "name_inp"

    <rule_like> NAME_2:
      input: "name_2_inp"
    """
    Given I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile: "boo.smk"

    use rule NAME from MODULE as NAME_3 with:
      threads: 1

    use rule NAME from MODULE as NAME_4 with:
      threads: 2

    use rule NAME_2 as INCORRECT_USE_CASE with:
      thread: 3

    use rule NAME, NAME_2 from MODULE as EXPLICIT_DEFINITION_OF_*

    use rule * from MODULE as IMPLICIT_DEFINITION_OF_*
    """
    When I change current file to <boo.smk>
    And I put the caret at NAME:
    Then I expect marker of overridden section with references:
      | NAME_3                   | foo.smk |
      | NAME_4                   | foo.smk |
      | EXPLICIT_DEFINITION_OF_* | foo.smk |
      | IMPLICIT_DEFINITION_OF_* | foo.smk |
    When I change current file to <boo.smk>
    And I put the caret at NAME_2:
    Then I expect marker of overridden section with references:
      | EXPLICIT_DEFINITION_OF_* | foo.smk |
      | IMPLICIT_DEFINITION_OF_* | foo.smk |
    When I change current file to <foo.smk>
    And I put the caret at NAME_3
    Then I expect marker of overriding section with references:
      | NAME | boo.smk |
    When I put the caret at NAME_4
    Then I expect marker of overriding section with references:
      | NAME | boo.smk |
    When I put the caret at INCORRECT_USE_CASE
    Then I expect no markers
    When I put the caret at EXPLICIT_DEFINITION_OF_*
    Then I expect marker of overriding section with references:
      | NAME | boo.smk |
      | NAME_2 | boo.smk |
    When I put the caret at IMPLICIT_DEFINITION_OF_*
    Then I expect marker of overriding section with references:
      | NAME   | boo.smk |
      | NAME_2 | boo.smk |
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |