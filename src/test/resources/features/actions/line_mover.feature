Feature: Line mover
  Scenario Outline: Permutation up/down between rules sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> <name2>:
        <rule_section2>: <content2>
    <rule_like1> <name1>:
        <rule_section1>:
            <content1>
    """
    When I put the caret at <rule_like1> <name1>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> <name1>:
        <rule_section1>:
            <content1>
    <rule_like2> <name2>:
        <rule_section2>: <content2>
    """
    When I put the caret at <rule_like1> <name1>:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> <name2>:
        <rule_section2>: <content2>
    <rule_like1> <name1>:
        <rule_section1>:
            <content1>
    """
    Examples:
      | rule_like1  | rule_like2  | rule_section1  | rule_section2  | content1    | name1 | name2   | content2   |
      | rule        | rule        | input          | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | checkpoint  | rule        | input          | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | subworkflow | rule        | workdir        | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | rule        | checkpoint  | input          | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | checkpoint  | checkpoint  | input          | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | subworkflow | checkpoint  | workdir        | output         | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | rule        | subworkflow | input          | workdir        | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | checkpoint  | subworkflow | input          | workdir        | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | subworkflow | subworkflow | workdir        | workdir        | "file.txt"  | NAME1 | NAME2   | "file.txt" |
      | rule        | def         | input          | n              | "file.txt"  | NAME1 | NAME2() | int        |
      | checkpoint  | def         | input          | n              | "file.txt"  | NAME1 | NAME2() | int        |
      | subworkflow | def         | workdir        | n              | "file.txt"  | NAME1 | NAME2() | int        |

  Scenario Outline: Permutation up/down between rule and comment sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <comment>
    <rule_like1> NAME1:
        <rule_section1>:
            <content>

    pass
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
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

  Scenario Outline: Permutation up/down inside rule section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
        <rule_section2>: <content2>
    """
    When I put the caret at <rule_section2>: <content2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section2>: <content2>
        <rule_section1>: <content1>

    """
    When I put the caret at <rule_section2>: <content2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
        <rule_section2>: <content2>

    """
    Examples:
      | rule_like1  | rule_section1  | rule_section2 | content1        | content2         |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/"             | "/"              |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/"             | "/"              |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/"             | "/"              |

  Scenario Outline: Permutation up/down inside rule section with complex content
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>:
                  <content1>
        <rule_section2>:
                   <content2>
    """
    When I put the caret at <content2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section2>:
                   <content2>
        <rule_section1>:
                  <content1>

    """
    When I put the caret at <content2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>:
                  <content1>
        <rule_section2>:
                   <content2>

    """
    Examples:
      | rule_like1  | rule_section1  | rule_section2 | content1        | content2         |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/dir"          | "/"              |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/dir"          | "/"              |
      | rule        | input          | output        | "file.txt"      | "file2.txt"      |
      | checkpoint  | input          | output        | "file.txt"      | "file2.txt"      |
      | subworkflow | workdir        | configfile    | "/dir"          | "/"              |

  Scenario Outline: Permutation up/down between rule and statement sections on top level
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <rule_section>:
                  <content>
    <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <statement_name>: <statement_content>
    <rule_like> NAME1:
        <rule_section>:
                  <content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>:
                  <content>
    <statement_name>: <statement_content>
    """
    Examples:
      | rule_like   | rule_section   | statement_name       | content         | statement_content |
      | rule        | input          | config               | "file.txt"      | "d"               |
      | rule        | input          | singularity          | "file.txt"      | ""                |
      | rule        | input          | workdir              | "file.txt"      | "/d"              |
      | rule        | input          | include              | "file.txt"      | ""                |
      | rule        | input          | localrules           | "file.txt"      | NAME1             |
      | rule        | input          | ruleorder            | "file.txt"      | NAME1             |
      | rule        | input          | snakefile            | "file.txt"      | ""                |
      | rule        | input          | configfile           | "file.txt"      | ""                |
      | checkpoint  | input          | config               | "file.txt"      | "d"               |
      | checkpoint  | input          | singularity          | "file.txt"      | ""                |
      | checkpoint  | input          | workdir              | "file.txt"      | "/d"              |
      | checkpoint  | input          | include              | "file.txt"      | ""                |
      | checkpoint  | input          | localrules           | "file.txt"      | NAME1             |
      | checkpoint  | input          | ruleorder            | "file.txt"      | NAME1             |
      | checkpoint  | input          | snakefile            | "file.txt"      | ""                |
      | checkpoint  | input          | configfile           | "file.txt"      | ""                |
      | subworkflow | workdir        | config               | "/dir"          | "d"               |
      | subworkflow | workdir        | singularity          | "/dir"          | ""                |
      | subworkflow | workdir        | include              | "/dir"          | ""                |
      | subworkflow | workdir        | localrules           | "/dir"          | NAME1             |
      | subworkflow | workdir        | ruleorder            | "/dir"          | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | "/dir"          | wildcard="/d+1"   |

  Scenario Outline: Move in/out rule
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <rule_section>:
            <content>
    <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>:
             <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>:
             <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <statement_name>: <statement_content>
    <rule_like> NAME1:
        <rule_section>:
             <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>:
             <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <rule_section>:
             <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <rule_section>:
             <content>
    <statement_name>: <statement_content>

    """
    Examples:
      | rule_like   | rule_section | statement_name       | content         | statement_content |
      | rule        | input        | wildcard_constraints | "file.txt"      | wildcard="/d+1"   |
      | checkpoint  | input        | wildcard_constraints | "file.txt"      | wildcard="/d+1"   |

  Scenario Outline: Move in/out doesn't work for last rule argument
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    Examples:
      | rule_like   | statement_name       | statement_content |
      | rule        | wildcard_constraints | wildcard="/d+1"   |
      | checkpoint  | wildcard_constraints | wildcard="/d+1"   |

  Scenario Outline: Move in/out if/else statement for rule like.
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    if True:
        pass
    else:
        pass
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    if True:
        pass
    else:
        <rule_like> NAME1:
            <statement_name>: <statement_content>

    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    if True:
        <rule_like> NAME1:
            <statement_name>: <statement_content>
    else:
        pass

    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    if True:
        pass
    else:
        pass

    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    if True:
        <rule_like> NAME1:
            <statement_name>: <statement_content>
    else:
        pass

    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    if True:
        pass
    else:
        <rule_like> NAME1:
            <statement_name>: <statement_content>

    """
    When I put the caret at <rule_like> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    if True:
        pass
    else:
        pass
    <rule_like> NAME1:
        <statement_name>: <statement_content>

    """
    Examples:
      | rule_like   | statement_name       | statement_content |
      | rule        | wildcard_constraints | wildcard="/d+1"   |
      | checkpoint  | wildcard_constraints | wildcard="/d+1"   |
      | subworkflow | workdir              | "/dir"            |
