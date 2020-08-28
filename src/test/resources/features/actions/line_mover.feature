Feature: Line mover
  Scenario Outline: Swap rule like toplevel sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    """
    Examples:
      | rule_like1  | rule_like2  | rule_section1  | rule_section2  | content1       | content2      |
      | rule        | rule        | input          | output         | "file1.txt"    | "file2.txt"   |
      | checkpoint  | rule        | input          | output         | "file1.txt"    | "file2.txt"   |
      | subworkflow | rule        | workdir        | output         | "file1.txt"    | "file2.txt"   |
      | rule        | checkpoint  | input          | output         | "file1.txt"    | "file2.txt"   |
      | checkpoint  | checkpoint  | input          | output         | "file1.txt"    | "file2.txt"   |
      | subworkflow | checkpoint  | workdir        | output         | "file1.txt"    | "file2.txt"   |
      | rule        | subworkflow | input          | workdir        | "file1.txt"    | "file2.txt"   |
      | checkpoint  | subworkflow | input          | workdir        | "file1.txt"    | "file2.txt"   |
      | subworkflow | subworkflow | workdir        | workdir        | "file1.txt"    | "file2.txt"   |
      | rule        | rule        | input          | output         | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | rule        | input          | output         | "file1.txt"    | \n"file2.txt" |
      | subworkflow | rule        | workdir        | output         | "file1.txt"    | \n"file2.txt" |
      | rule        | checkpoint  | input          | output         | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | checkpoint  | input          | output         | "file1.txt"    | \n"file2.txt" |
      | subworkflow | checkpoint  | workdir        | output         | "file1.txt"    | \n"file2.txt" |
      | rule        | subworkflow | input          | workdir        | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | subworkflow | input          | workdir        | "file1.txt"    | \n"file2.txt" |
      | subworkflow | subworkflow | workdir        | workdir        | "file1.txt"    | \n"file2.txt" |
      | rule        | rule        | input          | output         | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | rule        | input          | output         | \n"file1.txt"  | "file2.txt"   |
      | subworkflow | rule        | workdir        | output         | \n"file1.txt"  | "file2.txt"   |
      | rule        | checkpoint  | input          | output         | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | checkpoint  | input          | output         | \n"file1.txt"  | "file2.txt"   |
      | subworkflow | checkpoint  | workdir        | output         | \n"file1.txt"  | "file2.txt"   |
      | rule        | subworkflow | input          | workdir        | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | subworkflow | input          | workdir        | \n"file1.txt"  | "file2.txt"   |
      | subworkflow | subworkflow | workdir        | workdir        | \n"file1.txt"  | "file2.txt"   |
      | rule        | rule        | input          | output         | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | rule        | input          | output         | \n"file1.txt"  | \n"file2.txt" |
      | subworkflow | rule        | workdir        | output         | \n"file1.txt"  | \n"file2.txt" |
      | rule        | checkpoint  | input          | output         | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | checkpoint  | input          | output         | \n"file1.txt"  | \n"file2.txt" |
      | subworkflow | checkpoint  | workdir        | output         | \n"file1.txt"  | \n"file2.txt" |
      | rule        | subworkflow | input          | workdir        | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | subworkflow | input          | workdir        | \n"file1.txt"  | \n"file2.txt" |
      | subworkflow | subworkflow | workdir        | workdir        | \n"file1.txt"  | \n"file2.txt" |

  Scenario Outline: Swap rule like toplevel and python function sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <def>
        <def_section>: <def_content>
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    <def>
        <def_section>: <def_content>
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <def>
        <def_section>: <def_content>
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    """
    When I put the caret at <def>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    <def>
        <def_section>: <def_content>
    """
    When I put the caret at <def>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <def>
        <def_section>: <def_content>
    <rule_like1> NAME1:
        <rule_section1>: <content1>
    """
    Examples:
      | rule_like1  | def           | rule_section1  | def_section  | content1       | def_content   |
      | rule        | def NAME2():  | input          | n            | "file1.txt"    | int           |
      | checkpoint  | def NAME2():  | input          | n            | "file1.txt"    | int           |
      | subworkflow | def NAME2():  | workdir        | n            | "file1.txt"    | int           |

  Scenario Outline: Swap rule like section and comment
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    # comment
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
    # comment

    pass
    """
    When I put the caret at <rule_like1> NAME1:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    # comment
    <rule_like1> NAME1:
        <rule_section1>:
            <content>

    pass
    """
    Examples:
      | rule_like1  | rule_section1  | content |
      | rule        | input          | ""      |
      | checkpoint  | input          | ""      |
      | subworkflow | workdir        | ""      |
      | rule        | input          | ""      |
      | checkpoint  | input          | ""      |
      | subworkflow | workdir        | ""      |
      | rule        | input          | ""      |
      | checkpoint  | input          | ""      |
      | subworkflow | workdir        | ""      |
      | rule        | input          | \n""    |
      | checkpoint  | input          | \n""    |
      | subworkflow | workdir        | \n""    |
      | rule        | input          | \n""    |
      | checkpoint  | input          | \n""    |
      | subworkflow | workdir        | \n""    |
      | rule        | input          | \n""    |
      | checkpoint  | input          | \n""    |
      | subworkflow | workdir        | \n""    |

  Scenario Outline: Swap rule like single line subsections
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
      | rule        | input          | output        | "file.txt"      | \n"file2.txt"    |
      | checkpoint  | input          | output        | "file.txt"      | \n"file2.txt"    |
      | subworkflow | workdir        | configfile    | "/dir"          | \n"/"            |
      | rule        | input          | output        | "file.txt"      | \n"file2.txt"    |
      | checkpoint  | input          | output        | "file.txt"      | \n"file2.txt"    |
      | subworkflow | workdir        | configfile    | "/dir"          | \n"/"            |
      | rule        | input          | output        | "file.txt"      | \n"file2.txt"    |
      | checkpoint  | input          | output        | "file.txt"      | \n"file2.txt"    |
      | subworkflow | workdir        | configfile    | "/dir"          | \n"/"            |

  Scenario Outline: Swap rule like with toplevel section
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
      | rule        | input          | workdir              | "file.txt"      | "/d"              |
      | rule        | input          | include              | "file.txt"      | ""                |
      | rule        | input          | localrules           | "file.txt"      | NAME1             |
      | rule        | input          | ruleorder            | "file.txt"      | NAME1             |
      | rule        | input          | snakefile            | "file.txt"      | ""                |
      | rule        | input          | configfile           | "file.txt"      | ""                |
      | checkpoint  | input          | config               | "file.txt"      | "d"               |
      | checkpoint  | input          | workdir              | "file.txt"      | "/d"              |
      | checkpoint  | input          | include              | "file.txt"      | ""                |
      | checkpoint  | input          | localrules           | "file.txt"      | NAME1             |
      | checkpoint  | input          | ruleorder            | "file.txt"      | NAME1             |
      | checkpoint  | input          | snakefile            | "file.txt"      | ""                |
      | checkpoint  | input          | configfile           | "file.txt"      | ""                |
      | subworkflow | workdir        | config               | "/dir"          | "d"               |
      | subworkflow | workdir        | include              | "/dir"          | ""                |
      | subworkflow | workdir        | localrules           | "/dir"          | NAME1             |
      | subworkflow | workdir        | ruleorder            | "/dir"          | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | "/dir"          | wildcard="/d+1"   |
      | rule        | input          | config               | \n"file.txt"    | "d"               |
      | rule        | input          | workdir              | \n"file.txt"    | "/d"              |
      | rule        | input          | include              | \n"file.txt"    | ""                |
      | rule        | input          | localrules           | \n"file.txt"    | NAME1             |
      | rule        | input          | ruleorder            | \n"file.txt"    | NAME1             |
      | rule        | input          | snakefile            | \n"file.txt"    | ""                |
      | rule        | input          | configfile           | \n"file.txt"    | ""                |
      | checkpoint  | input          | config               | \n"file.txt"    | "d"               |
      | checkpoint  | input          | workdir              | \n"file.txt"    | "/d"              |
      | checkpoint  | input          | include              | \n"file.txt"    | ""                |
      | checkpoint  | input          | localrules           | \n"file.txt"    | NAME1             |
      | checkpoint  | input          | ruleorder            | \n"file.txt"    | NAME1             |
      | checkpoint  | input          | snakefile            | \n"file.txt"    | ""                |
      | checkpoint  | input          | configfile           | \n"file.txt"    | ""                |
      | subworkflow | workdir        | config               | \n"/dir"        | "d"               |
      | subworkflow | workdir        | include              | \n"/dir"        | ""                |
      | subworkflow | workdir        | localrules           | \n"/dir"        | NAME1             |
      | subworkflow | workdir        | ruleorder            | \n"/dir"        | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | \n"/dir"        | wildcard="/d+1"   |
      | rule        | input          | config               | \n"file.txt"\n  | "d"               |
      | rule        | input          | workdir              | \n"file.txt"\n  | "/d"              |
      | rule        | input          | include              | \n"file.txt"\n  | ""                |
      | rule        | input          | localrules           | \n"file.txt"\n  | NAME1             |
      | rule        | input          | ruleorder            | \n"file.txt"\n  | NAME1             |
      | rule        | input          | snakefile            | \n"file.txt"\n  | ""                |
      | rule        | input          | configfile           | \n"file.txt"\n  | ""                |
      | checkpoint  | input          | config               | \n"file.txt"\n  | "d"               |
      | checkpoint  | input          | workdir              | \n"file.txt"\n  | "/d"              |
      | checkpoint  | input          | include              | \n"file.txt"\n  | ""                |
      | checkpoint  | input          | localrules           | \n"file.txt"\n  | NAME1             |
      | checkpoint  | input          | ruleorder            | \n"file.txt"\n  | NAME1             |
      | checkpoint  | input          | snakefile            | \n"file.txt"\n  | ""                |
      | checkpoint  | input          | configfile           | \n"file.txt"\n  | ""                |
      | subworkflow | workdir        | config               | \n"/dir"\n      | "d"               |
      | subworkflow | workdir        | include              | \n"/dir"\n      | ""                |
      | subworkflow | workdir        | localrules           | \n"/dir"\n      | NAME1             |
      | subworkflow | workdir        | ruleorder            | \n"/dir"\n      | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | \n"/dir"\n      | wildcard="/d+1"   |

  Scenario Outline: Move section in/out rule (e.g. wildcards)
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <rule_section>: <content>
    <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>: <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>: <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <statement_name>: <statement_content>
    <rule_like> NAME1:
        <rule_section>: <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>: <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <rule_section>: <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1:
        <rule_section>: <content>
    <statement_name>: <statement_content>

    """
    Examples:
      | rule_like   | rule_section | statement_name       | content         | statement_content |
      | rule        | input        | wildcard_constraints | "file.txt"      | wildcard="/d+1"   |
      | checkpoint  | input        | wildcard_constraints | "file.txt"      | wildcard="/d+1"   |
      | rule        | input        | wildcard_constraints | \n"file.txt"    | wildcard="/d+1"   |
      | checkpoint  | input        | wildcard_constraints | \n"file.txt"    | wildcard="/d+1"   |
      | rule        | input        | wildcard_constraints | "file.txt"      | \nwildcard="/d+1" |
      | checkpoint  | input        | wildcard_constraints | "file.txt"      | \nwildcard="/d+1" |
      | rule        | input        | wildcard_constraints | \n"file.txt"    | \nwildcard="/d+1" |
      | checkpoint  | input        | wildcard_constraints | \n"file.txt"    | \nwildcard="/d+1" |

  Scenario Outline: Move section in/out rule doesn't work for rule with one section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
    """
    Examples:
      | rule_like   | statement_name         | statement_content |
      | rule        | wildcard_constraints   | wildcard="/d+1"   |
      | checkpoint  | wildcard_constraints   | wildcard="/d+1"   |
      | subworkflow | workdir                | "/dir"            |
      | rule        | wildcard_constraints   | \nwildcard="/d+1" |
      | checkpoint  | wildcard_constraints   | \nwildcard="/d+1" |
      | subworkflow | workdir                | \n"/dir"          |
      | rule        | \nwildcard_constraints | wildcard="/d+1"   |
      | checkpoint  | \nwildcard_constraints | wildcard="/d+1"   |
      | subworkflow | \nworkdir              | "/dir"            |
      | rule        | \nwildcard_constraints | \nwildcard="/d+1" |
      | checkpoint  | \nwildcard_constraints | \nwildcard="/d+1" |
      | subworkflow | \nworkdir              | \n"/dir"          |

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

  Scenario Outline: Move section inside rule
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <rule_section>: <content>
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>: <content>

    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        <rule_section>: <content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>: <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>: <content>
        <statement_name>: <statement_content>

    """
    Examples:
      | rule_like   | rule_section | statement_name       | content         | statement_content |
      | rule        | input        | input                | "file.txt"      | ""                |
      | rule        | input        | output               | "file.txt"      | ""                |
      | rule        | input        | shell                | "file.txt"      | ""                |
      | rule        | input        | log                  | "file.txt"      | ""                |
      | rule        | input        | params               | "file.txt"      | ""                |
      | rule        | input        | resources            | "file.txt"      | ""                |
      | rule        | input        | version              | "file.txt"      | ""                |
      | rule        | input        | message              | "file.txt"      | ""                |
      | rule        | input        | benchmark            | "file.txt"      | ""                |
      | rule        | input        | priority             | "file.txt"      | ""                |
      | rule        | input        | wrapper              | "file.txt"      | ""                |
      | rule        | input        | input                | \n"file.txt"    | ""                |
      | rule        | input        | output               | \n"file.txt"    | ""                |
      | rule        | input        | shell                | \n"file.txt"    | ""                |
      | rule        | input        | log                  | \n"file.txt"    | ""                |
      | rule        | input        | params               | \n"file.txt"    | ""                |
      | rule        | input        | resources            | \n"file.txt"    | ""                |
      | rule        | input        | version              | \n"file.txt"    | ""                |
      | rule        | input        | message              | \n"file.txt"    | ""                |
      | rule        | input        | benchmark            | \n"file.txt"    | ""                |
      | rule        | input        | priority             | \n"file.txt"    | ""                |
      | rule        | input        | wrapper              | \n"file.txt"    | ""                |
      | rule        | input        | input                | "file.txt"      | \n""              |
      | rule        | input        | output               | "file.txt"      | \n""              |
      | rule        | input        | shell                | "file.txt"      | \n""              |
      | rule        | input        | log                  | "file.txt"      | \n""              |
      | rule        | input        | params               | "file.txt"      | \n""              |
      | rule        | input        | resources            | "file.txt"      | \n""              |
      | rule        | input        | version              | "file.txt"      | \n""              |
      | rule        | input        | message              | "file.txt"      | \n""              |
      | rule        | input        | benchmark            | "file.txt"      | \n""              |
      | rule        | input        | priority             | "file.txt"      | \n""              |
      | rule        | input        | wrapper              | "file.txt"      | \n""              |
      | rule        | input        | input                | \n"file.txt"    | \n""              |
      | rule        | input        | output               | \n"file.txt"    | \n""              |
      | rule        | input        | shell                | \n"file.txt"    | \n""              |
      | rule        | input        | log                  | \n"file.txt"    | \n""              |
      | rule        | input        | params               | \n"file.txt"    | \n""              |
      | rule        | input        | resources            | \n"file.txt"    | \n""              |
      | rule        | input        | version              | \n"file.txt"    | \n""              |
      | rule        | input        | message              | \n"file.txt"    | \n""              |
      | rule        | input        | benchmark            | \n"file.txt"    | \n""              |
      | rule        | input        | priority             | \n"file.txt"    | \n""              |
      | rule        | input        | wrapper              | \n"file.txt"    | \n""              |
      | checkpoint  | input        | input                | "file.txt"      | ""                |
      | checkpoint  | input        | output               | "file.txt"      | ""                |
      | checkpoint  | input        | shell                | "file.txt"      | ""                |
      | checkpoint  | input        | log                  | "file.txt"      | ""                |
      | checkpoint  | input        | params               | "file.txt"      | ""                |
      | checkpoint  | input        | resources            | "file.txt"      | ""                |
      | checkpoint  | input        | version              | "file.txt"      | ""                |
      | checkpoint  | input        | message              | "file.txt"      | ""                |
      | checkpoint  | input        | benchmark            | "file.txt"      | ""                |
      | checkpoint  | input        | priority             | "file.txt"      | ""                |
      | checkpoint  | input        | wrapper              | "file.txt"      | ""                |
      | checkpoint  | input        | input                | \n"file.txt"    | ""                |
      | checkpoint  | input        | output               | \n"file.txt"    | ""                |
      | checkpoint  | input        | shell                | \n"file.txt"    | ""                |
      | checkpoint  | input        | log                  | \n"file.txt"    | ""                |
      | checkpoint  | input        | params               | \n"file.txt"    | ""                |
      | checkpoint  | input        | resources            | \n"file.txt"    | ""                |
      | checkpoint  | input        | version              | \n"file.txt"    | ""                |
      | checkpoint  | input        | message              | \n"file.txt"    | ""                |
      | checkpoint  | input        | benchmark            | \n"file.txt"    | ""                |
      | checkpoint  | input        | priority             | \n"file.txt"    | ""                |
      | checkpoint  | input        | wrapper              | \n"file.txt"    | ""                |
      | checkpoint  | input        | input                | "file.txt"      | \n""              |
      | checkpoint  | input        | output               | "file.txt"      | \n""              |
      | checkpoint  | input        | shell                | "file.txt"      | \n""              |
      | checkpoint  | input        | log                  | "file.txt"      | \n""              |
      | checkpoint  | input        | params               | "file.txt"      | \n""              |
      | checkpoint  | input        | resources            | "file.txt"      | \n""              |
      | checkpoint  | input        | version              | "file.txt"      | \n""              |
      | checkpoint  | input        | message              | "file.txt"      | \n""              |
      | checkpoint  | input        | benchmark            | "file.txt"      | \n""              |
      | checkpoint  | input        | priority             | "file.txt"      | \n""              |
      | checkpoint  | input        | wrapper              | "file.txt"      | \n""              |
      | checkpoint  | input        | input                | \n"file.txt"    | \n""              |
      | checkpoint  | input        | output               | \n"file.txt"    | \n""              |
      | checkpoint  | input        | shell                | \n"file.txt"    | \n""              |
      | checkpoint  | input        | log                  | \n"file.txt"    | \n""              |
      | checkpoint  | input        | params               | \n"file.txt"    | \n""              |
      | checkpoint  | input        | resources            | \n"file.txt"    | \n""              |
      | checkpoint  | input        | version              | \n"file.txt"    | \n""              |
      | checkpoint  | input        | message              | \n"file.txt"    | \n""              |
      | checkpoint  | input        | benchmark            | \n"file.txt"    | \n""              |
      | checkpoint  | input        | priority             | \n"file.txt"    | \n""              |
      | checkpoint  | input        | wrapper              | \n"file.txt"    | \n""              |

  Scenario Outline: Swap rule between rule sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>

    <comment>

    <rule_like1> NAME1:
        input: <content1>
        output: <content1>
    """
    When I put the caret at input: <content1>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>
        input: <content1>

    <comment>

    <rule_like1> NAME1:
        output: <content1>
    """
    When I put the caret at input: <content1>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>

    <comment>

    <rule_like1> NAME1:
        input: <content1>
        output: <content1>
    """
    Examples:
      | rule_like1  | rule_like2  | content1       | content2      | comment   |
      | rule        | rule        | "file1.txt"    | "file2.txt"   | # comment |
      | checkpoint  | rule        | "file1.txt"    | "file2.txt"   | # comment |
      | rule        | checkpoint  | "file1.txt"    | "file2.txt"   | # comment |
      | checkpoint  | checkpoint  | "file1.txt"    | "file2.txt"   | # comment |
      | rule        | rule        | "file1.txt"    | \n"file2.txt" | # comment |
      | checkpoint  | rule        | "file1.txt"    | \n"file2.txt" | # comment |
      | rule        | checkpoint  | "file1.txt"    | \n"file2.txt" | # comment |
      | checkpoint  | checkpoint  | "file1.txt"    | \n"file2.txt" | # comment |
      | rule        | rule        | \n"file1.txt"  | "file2.txt"   | # comment |
      | checkpoint  | rule        | \n"file1.txt"  | "file2.txt"   | # comment |
      | rule        | checkpoint  | \n"file1.txt"  | "file2.txt"   | # comment |
      | checkpoint  | checkpoint  | \n"file1.txt"  | "file2.txt"   | # comment |
      | rule        | rule        | \n"file1.txt"  | \n"file2.txt" | # comment |
      | checkpoint  | rule        | \n"file1.txt"  | \n"file2.txt" | # comment |
      | rule        | checkpoint  | \n"file1.txt"  | \n"file2.txt" | # comment |
      | checkpoint  | checkpoint  | \n"file1.txt"  | \n"file2.txt" | # comment |
      | rule        | rule        | "file1.txt"    | "file2.txt"   |           |
      | checkpoint  | rule        | "file1.txt"    | "file2.txt"   |           |
      | rule        | checkpoint  | "file1.txt"    | "file2.txt"   |           |
      | checkpoint  | checkpoint  | "file1.txt"    | "file2.txt"   |           |
      | rule        | rule        | "file1.txt"    | \n"file2.txt" |           |
      | checkpoint  | rule        | "file1.txt"    | \n"file2.txt" |           |
      | rule        | checkpoint  | "file1.txt"    | \n"file2.txt" |           |
      | checkpoint  | checkpoint  | "file1.txt"    | \n"file2.txt" |           |
      | rule        | rule        | \n"file1.txt"  | "file2.txt"   |           |
      | checkpoint  | rule        | \n"file1.txt"  | "file2.txt"   |           |
      | rule        | checkpoint  | \n"file1.txt"  | "file2.txt"   |           |
      | checkpoint  | checkpoint  | \n"file1.txt"  | "file2.txt"   |           |
      | checkpoint  | rule        | \n"file1.txt"  | \n"file2.txt" |           |
      | rule        | checkpoint  | \n"file1.txt"  | \n"file2.txt" |           |
      | checkpoint  | checkpoint  | \n"file1.txt"  | \n"file2.txt" |           |
      | rule        | rule        | \n"file1.txt"  | \n"file2.txt" |           |

  Scenario Outline: Swap rule between rule sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>

    pass

    <rule_like1> NAME1:
        input: <content1>
        output: <content1>
    """
    When I put the caret at input: <content1>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>

    pass

    <rule_like1> NAME1:
        input: <content1>
        output: <content1>
    """
    When I put the caret at output: <content2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2:
        input: <content2>
        output: <content2>

    pass

    <rule_like1> NAME1:
        input: <content1>
        output: <content1>
    """
    Examples:
      | rule_like1  | rule_like2  | content1       | content2      |
      | rule        | rule        | "file1.txt"    | "file2.txt"   |
      | checkpoint  | rule        | "file1.txt"    | "file2.txt"   |
      | rule        | checkpoint  | "file1.txt"    | "file2.txt"   |
      | checkpoint  | checkpoint  | "file1.txt"    | "file2.txt"   |
      | rule        | rule        | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | rule        | "file1.txt"    | \n"file2.txt" |
      | rule        | checkpoint  | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | checkpoint  | "file1.txt"    | \n"file2.txt" |
      | rule        | rule        | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | rule        | \n"file1.txt"  | "file2.txt"   |
      | rule        | checkpoint  | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | checkpoint  | \n"file1.txt"  | "file2.txt"   |
      | rule        | rule        | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | rule        | \n"file1.txt"  | \n"file2.txt" |
      | rule        | checkpoint  | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | checkpoint  | \n"file1.txt"  | \n"file2.txt" |
      | rule        | rule        | "file1.txt"    | "file2.txt"   |
      | checkpoint  | rule        | "file1.txt"    | "file2.txt"   |
      | rule        | checkpoint  | "file1.txt"    | "file2.txt"   |
      | checkpoint  | checkpoint  | "file1.txt"    | "file2.txt"   |
      | rule        | rule        | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | rule        | "file1.txt"    | \n"file2.txt" |
      | rule        | checkpoint  | "file1.txt"    | \n"file2.txt" |
      | checkpoint  | checkpoint  | "file1.txt"    | \n"file2.txt" |
      | rule        | rule        | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | rule        | \n"file1.txt"  | "file2.txt"   |
      | rule        | checkpoint  | \n"file1.txt"  | "file2.txt"   |
      | checkpoint  | checkpoint  | \n"file1.txt"  | "file2.txt"   |
      | rule        | rule        | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | rule        | \n"file1.txt"  | \n"file2.txt" |
      | rule        | checkpoint  | \n"file1.txt"  | \n"file2.txt" |
      | checkpoint  | checkpoint  | \n"file1.txt"  | \n"file2.txt" |

  Scenario Outline: Swap python functions inside run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like>
      <run_section>:
                     <section1>
                         <section1_content>
                     <section2>
                         <section2_content>
    """
    When I put the caret at <section2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like>
      <run_section>:
                     <section2>
                         <section2_content>
                     <section1>
                         <section1_content>
    """
    When I put the caret at <section2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like>
      <run_section>:
                     <section1>
                         <section1_content>
                     <section2>
                         <section2_content>
    """
    Examples:
      | rule_like | run_section | section1    | section1_content | section2  | section2_content |
      | rule      | run         | def f1():   | pass             | def f2(): | pass             |
      | rule      | run         | def f1():   | a: int           | def f2(): | b: int           |

  Scenario Outline: Swap sections in python functions inside run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like>
      <run_section>:
                     <section>
                         <section_content1>
                         <section_content2>
    """
    When I put the caret at <section_content2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like>
      <run_section>:
                     <section>
                         <section_content2>
                         <section_content1>

    """
    When I put the caret at <section_content2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like>
      <run_section>:
                     <section>
                         <section_content1>
                         <section_content2>

    """
    Examples:
      | rule_like | run_section | section     | section_content1                 | section_content2         |
      | rule      | run         | def f1():   | pass                             | b: int                   |
      | rule      | run         | def f1():   | a: int                           | b: int                   |

  Scenario: Complex example for swap sections in python functions inside run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule NAME:
      run:
            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                    return flag(path, "subworkflow", self)
                return [self.target(path) for path in paths]
    """
    When I put the caret at return flag(path, "subworkflow", self)
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    rule NAME:
      run:
            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    return flag(path, "subworkflow", self)
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                return [self.target(path) for path in paths]
    """
    When I put the caret at return flag(path, "subworkflow", self)
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    rule NAME:
      run:
            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                    return flag(path, "subworkflow", self)
                return [self.target(path) for path in paths]
    """

  Scenario: Complex example for moving python functions inside run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule NAME:
      run:
            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                    return flag(path, "subworkflow", self)
                return [self.target(path) for path in paths]

            def targets(self, dag):
              def relpath(f):
                  if f.startswith(self.workdir):
                      return os.path.relpath(f, start=self.workdir)
                  # do not adjust absolute targets outside of workdir
                  return f
              return [
                  relpath(f)
                  for job in dag.jobs
                  for f in job.subworkflow_input
                  if job.subworkflow_input[f] is self
              ]
    """
    When I put the caret at def targets(self, dag):
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    rule NAME:
      run:
            def targets(self, dag):
              def relpath(f):
                  if f.startswith(self.workdir):
                      return os.path.relpath(f, start=self.workdir)
                  # do not adjust absolute targets outside of workdir
                  return f
              return [
                  relpath(f)
                  for job in dag.jobs
                  for f in job.subworkflow_input
                  if job.subworkflow_input[f] is self
              ]

            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                    return flag(path, "subworkflow", self)
                return [self.target(path) for path in paths]
    """
    When I put the caret at def targets(self, dag):
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    rule NAME:
      run:
            def target(self, paths):
                if not_iterable(paths):
                    path = paths
                    path = (
                        path
                        if os.path.isabs(path) or path.startswith("root://")
                        else os.path.join(self.workdir, path)
                    )
                    return flag(path, "subworkflow", self)
                return [self.target(path) for path in paths]

            def targets(self, dag):
              def relpath(f):
                  if f.startswith(self.workdir):
                      return os.path.relpath(f, start=self.workdir)
                  # do not adjust absolute targets outside of workdir
                  return f
              return [
                  relpath(f)
                  for job in dag.jobs
                  for f in job.subworkflow_input
                  if job.subworkflow_input[f] is self
              ]
    """