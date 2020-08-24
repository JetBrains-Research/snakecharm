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
      | rule_like1  | rule_like2  | rule_section1  | rule_section2  | content         |
      | rule        | rule        | input          | output         | "file.txt"      |
      | checkpoint  | rule        | input          | output         | "file.txt"      |
      | subworkflow | rule        | workdir        | output         | "file.txt"      |
      | rule        | checkpoint  | input          | output         | "file.txt"      |
      | checkpoint  | checkpoint  | input          | output         | "file.txt"      |
      | subworkflow | checkpoint  | workdir        | output         | "file.txt"      |
      | rule        | subworkflow | input          | workdir        | "file.txt"      |
      | checkpoint  | subworkflow | input          | workdir        | "file.txt"      |
      | subworkflow | subworkflow | workdir        | workdir        | "file.txt"      |

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

  Scenario Outline: Permutation down between rule and comment sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>:
            <content>
    <comment>

    pass
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <comment>
    <rule_like1> NAME1:
        <rule_section1>:
            <content>

    pass
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
