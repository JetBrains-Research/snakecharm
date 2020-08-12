Feature: Line mover
  Scenario Outline: Move rules up
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>: <content>
    <rule_like2> NAME2:
        <rule_section2>:
            <content>
    """
    When I put the caret at <rule_like2> NAME2:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like2> NAME2:
        <rule_section2>:
            <content>
    <rule_like1> NAME1:
        <rule_section1>: <content>

    """
    Examples:
      | rule_like1  | rule_like2  | rule_section1  | rule_section2  | content |
      | rule        | rule        | input          | output         | ""      |
      | checkpoint  | rule        | input          | output         | ""      |
      | subworkflow | rule        | workdir        | output         | ""      |
      | rule        | checkpoint  | input          | output         | ""      |
      | checkpoint  | checkpoint  | input          | output         | ""      |
      | subworkflow | checkpoint  | workdir        | output         | ""      |
      | rule        | subworkflow | input          | workdir        | ""      |
      | checkpoint  | subworkflow | input          | workdir        | ""      |
      | subworkflow | subworkflow | workdir        | workdir        | ""      |

  Scenario Outline: Move rules down
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>: <content>
    <rule_like2> NAME2:
        <rule_section2>:
            <content>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2:
        <rule_section2>:
            <content>
    <rule_like1> NAME1:
        <rule_section1>: <content>

    """
    Examples:
      | rule_like1  | rule_like2  | rule_section1  | rule_section2  | content |
      | rule        | rule        | input          | output         | ""      |
      | checkpoint  | rule        | input          | output         | ""      |
      | subworkflow | rule        | workdir        | output         | ""      |
      | rule        | checkpoint  | input          | output         | ""      |
      | checkpoint  | checkpoint  | input          | output         | ""      |
      | subworkflow | checkpoint  | workdir        | output         | ""      |
      | rule        | subworkflow | input          | workdir        | ""      |
      | checkpoint  | subworkflow | input          | workdir        | ""      |
      | subworkflow | subworkflow | workdir        | workdir        | ""      |

  Scenario Outline: Permutation up between rule and comment sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <comment>
    <rule_like1> NAME1:
        <rule_section1>:
            <content>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>:
            <content>
    <comment>

    """
    Examples:
      | rule_like1  | rule_section1  | content | comment   |
      | rule        | input          | ""      | # comment |
      | checkpoint  | input          | ""      | # comment |
      | subworkflow | workdir        | ""      | # comment |
      | rule        | input          | ""      | # comment |
      | checkpoint  | input          | ""      | # comment |
      | subworkflow | workdir        | ""      | # comment |
      | rule        | input          | ""      | # comment |
      | checkpoint  | input          | ""      | # comment |
      | subworkflow | workdir        | ""      | # comment |
