Feature: Statement mover
  Issue 174
   # TODO: cleanup case 1 ... 4
  
  Scenario Outline: Swap rule like toplevel sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    <rule_like1> NAME1 <addition>:
        <rule_section1>: <content1>
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1 <addition>:
        <rule_section1>: <content1>
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2:
        <rule_section2>: <content2>
    <rule_like1> NAME1 <addition>:
        <rule_section1>: <content1>
    """
    Examples:
      | rule_like1  | rule_like2  | rule_section1 | rule_section2 | content1               | content2              | addition      |
      | rule        | rule        | input         | output        | "file1.txt"            | "file2.txt"           |               |
      | checkpoint  | rule        | input         | output        | "file1.txt"            | "file2.txt"           |               |
      | subworkflow | rule        | workdir       | output        | "file1.txt"            | "file2.txt"           |               |
      | module      | rule        | snakefile     | output        | "file1.smk"            | "file2.txt"           |               |
      | use rule    | rule        | input         | output        | "file1.txt"            | "file2.txt"           | as NAME3 with |
      | rule        | checkpoint  | input         | output        | "file1.txt"            | "file2.txt"           |               |
      | checkpoint  | checkpoint  | input         | output        | "file1.txt"            | "file2.txt"           |               |
      | subworkflow | checkpoint  | workdir       | output        | "file1.txt"            | "file2.txt"           |               |
      | rule        | subworkflow | input         | workdir       | "file1.txt"            | "file2.txt"           |               |
      | checkpoint  | subworkflow | input         | workdir       | "file1.txt"            | "file2.txt"           |               |
      | subworkflow | subworkflow | workdir       | workdir       | "file1.txt"            | "file2.txt"           |               |
      | rule        | rule        | input         | output        | "file1.txt"            | \n        "file2.txt" |               |
      | checkpoint  | rule        | input         | output        | "file1.txt"            | \n        "file2.txt" |               |
      | subworkflow | rule        | workdir       | output        | "file1.txt"            | \n        "file2.txt" |               |
      | module      | rule        | snakefile     | output        | "file1.smk"            | \n        "file2.txt" |               |
      | use rule    | rule        | input         | output        | "file1.txt"            | \n        "file2.txt" | as NAME3 with |
      | rule        | checkpoint  | input         | output        | "file1.txt"            | \n        "file2.txt" |               |
      | checkpoint  | checkpoint  | input         | output        | "file1.txt"            | \n        "file2.txt" |               |
      | subworkflow | checkpoint  | workdir       | output        | "file1.txt"            | \n        "file2.txt" |               |
      | rule        | subworkflow | input         | workdir       | "file1.txt"            | \n        "file2.txt" |               |
      | checkpoint  | subworkflow | input         | workdir       | "file1.txt"            | \n        "file2.txt" |               |
      | subworkflow | subworkflow | workdir       | workdir       | "file1.txt"            | \n        "file2.txt" |               |
      | rule        | rule        | input         | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | checkpoint  | rule        | input         | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | subworkflow | rule        | workdir       | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | module      | rule        | snakefile     | output        | \n        "file1.smk"  | "file2.txt"           |               |
      | use rule    | rule        | input         | output        | \n         "file1.txt" | "file2.txt"           | as NAME3 with |
      | rule        | checkpoint  | input         | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | checkpoint  | checkpoint  | input         | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | subworkflow | checkpoint  | workdir       | output        | \n        "file1.txt"  | "file2.txt"           |               |
      | rule        | subworkflow | input         | workdir       | \n        "file1.txt"  | "file2.txt"           |               |
      | checkpoint  | subworkflow | input         | workdir       | \n        "file1.txt"  | "file2.txt"           |               |
      | subworkflow | subworkflow | workdir       | workdir       | \n        "file1.txt"  | "file2.txt"           |               |
      | rule        | rule        | input         | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | checkpoint  | rule        | input         | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | subworkflow | rule        | workdir       | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | module      | rule        | snakefile     | output        | \n        "file1.smk"  | \n        "file2.txt" |               |
      | use rule    | rule        | input         | output        | \n        "file1.txt"  | \n        "file2.txt" | as NAME3 with |
      | rule        | checkpoint  | input         | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | checkpoint  | checkpoint  | input         | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | subworkflow | checkpoint  | workdir       | output        | \n        "file1.txt"  | \n        "file2.txt" |               |
      | rule        | subworkflow | input         | workdir       | \n        "file1.txt"  | \n        "file2.txt" |               |
      | checkpoint  | subworkflow | input         | workdir       | \n        "file1.txt"  | \n        "file2.txt" |               |
      | subworkflow | subworkflow | workdir       | workdir       | \n        "file1.txt"  | \n        "file2.txt" |               |

  Scenario Outline: Swap rule like toplevel and python function sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    def NAME2():
        n = 1
    <rule_like1> NAME1 <addition>:
        <rule_section1>: "file1.txt"
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1 <addition>:
        <rule_section1>: "file1.txt"
    def NAME2():
        n = 1
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    def NAME2():
        n = 1
    <rule_like1> NAME1 <addition>:
        <rule_section1>: "file1.txt"
    """
    When I put the caret at def NAME2()
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like1> NAME1 <addition>:
        <rule_section1>: "file1.txt"
    def NAME2():
        n = 1
    """
    When I put the caret at def NAME2()
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    def NAME2():
        n = 1
    <rule_like1> NAME1 <addition>:
        <rule_section1>: "file1.txt"
    """
    Examples:
      | rule_like1  | rule_section1 | addition      |
      | rule        | input         |               |
      | checkpoint  | input         |               |
      | subworkflow | workdir       |               |
      | module      | snakefile     |               |
      | use rule    | input         | as NAME2 with |

  Scenario Outline: Swap rule like section and comment
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    # comment
    <rule_like1> NAME1 <addition>:
        <rule_section1>:
            <content>

    pass
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1 <addition>:
        <rule_section1>:
            <content>
    # comment

    pass
    """
    When I put the caret at <rule_like1> NAME1 <addition>:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    # comment
    <rule_like1> NAME1 <addition>:
        <rule_section1>:
            <content>

    pass
    """
    Examples:
      | rule_like1  | rule_section1 | content      | addition      |
      | rule        | input         | ""           |               |
      | checkpoint  | input         | ""           |               |
      | subworkflow | workdir       | ""           |               |
      | module      | snakefile     | ""           |               |
      | use rule    | input         | ""           | as NAME2 with |
      | rule        | input         | \n        "" |               |
      | checkpoint  | input         | \n        "" |               |
      | subworkflow | workdir       | \n        "" |               |
      | module      | snakefile     | \n        "" |               |
      | use rule    | input         | \n        "" | as NAME2 with |

  Scenario Outline: Swap rule like single line subsections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like1> NAME1:
        <rule_section1>: <content1>
        <rule_section2>: <content2>
    """
    When I put the caret at <rule_section2>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like1> NAME1:
        <rule_section2>: <content2>
        <rule_section1>: <content1>

    """
    When I put the caret at <rule_section2>:
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
      | rule        | input          | output        | "file.txt"      | \n        "file2.txt"    |
      | checkpoint  | input          | output        | "file.txt"      | \n        "file2.txt"    |
      | subworkflow | workdir        | configfile    | "/dir"          | \n        "/"            |

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
      | checkpoint  | input          | ruleorder            | "file.txt"      | NAME1             |
      | subworkflow | workdir        | config               | "/dir"          | "d"               |
      | subworkflow | workdir        | include              | "/dir"          | ""                |
      | subworkflow | workdir        | localrules           | "/dir"          | NAME1             |
      | subworkflow | workdir        | ruleorder            | "/dir"          | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | "/dir"          | wildcard="/d+1"   |
      | rule        | input          | config               | \n        "file.txt"    | "d"               |
      | rule        | input          | workdir              | \n        "file.txt"    | "/d"              |
      | rule        | input          | include              | \n        "file.txt"    | ""                |
      | rule        | input          | localrules           | \n        "file.txt"    | NAME1             |
      | rule        | input          | ruleorder            | \n        "file.txt"    | NAME1             |
      | rule        | input          | snakefile            | \n        "file.txt"    | ""                |
      | rule        | input          | configfile           | \n        "file.txt"    | ""                |
      | checkpoint  | input          | config               | \n        "file.txt"    | "d"               |
      | checkpoint  | input          | workdir              | \n        "file.txt"    | "/d"              |
      | subworkflow | workdir        | config               | \n        "/dir"        | "d"               |
      | subworkflow | workdir        | include              | \n        "/dir"        | ""                |
      | subworkflow | workdir        | localrules           | \n        "/dir"        | NAME1             |
      | subworkflow | workdir        | ruleorder            | \n        "/dir"        | NAME1             |
      | subworkflow | workdir        | wildcard_constraints | \n        "/dir"        | wildcard="/d+1"   |

  Scenario Outline: Swap toplevel section with empty line
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        <rule_section>:
            <content>
    
    <statement_name>: <statement_content>
    <text_below>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>:
            <content>
    <statement_name>: <statement_content>

    <text_below>
    """
    When I put the caret at <statement_name>: <statement_content>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        <rule_section>:
            <content>
    
    <statement_name>: <statement_content>
    <text_below>
    """
    Examples:
      | rule_like   | rule_section | statement_name       | content              | statement_content | text_below   |
      | rule        | input        | config               | "file.txt"           | "d"               | config "foo" |
      | rule        | input        | config               | "file.txt"           | "d"               |              |
      | rule        | input        | config               | \n        "file.txt" | "d"               | config "foo" |
      | rule        | input        | config               | \n        "file.txt" | "d"               |              |
      | rule        | input        | workdir              | \n        "file.txt" | "/d"              |              |
      | rule        | input        | include              | \n        "file.txt" | ""                |              |
      | rule        | input        | localrules           | \n        "file.txt" | NAME1             |              |
      | rule        | input        | ruleorder            | \n        "file.txt" | NAME1             |              |
      | rule        | input        | snakefile            | \n        "file.txt" | ""                |              |
      | rule        | input        | configfile           | \n        "file.txt" | ""                |              |
      | checkpoint  | input        | config               | \n        "file.txt" | "d"               |              |
      | checkpoint  | input        | workdir              | \n        "file.txt" | "/d"              |              |
      | subworkflow | workdir      | config               | \n        "/dir"     | "d"               |              |
      | subworkflow | workdir      | include              | \n        "/dir"     | ""                |              |
      | subworkflow | workdir      | localrules           | \n        "/dir"     | NAME1             |              |
      | subworkflow | workdir      | ruleorder            | \n        "/dir"     | NAME1             |              |
      | subworkflow | workdir      | wildcard_constraints | \n        "/dir"     | wildcard="/d+1"   |              |

  Scenario Outline: Move wildcard_constraints in/out rule (wildcard_constraints single line)
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1 <addition>:
        input:<content>
    wildcard_constraints:<statement_content>
    """
    When I put the caret at <caret>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1 <addition>:
        input:<content>
        wildcard_constraints:<statement_content>

    """
    When I put the caret at <caret>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1 <addition>:
        wildcard_constraints:<statement_content>
        input:<content>

    """
    When I put the caret at <caret>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    wildcard_constraints:<statement_content>
    <rule_like> NAME1 <addition>:
        input:<content>

    """
    When I put the caret at <caret>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1 <addition>:
        wildcard_constraints:<statement_content>
        input:<content>

    """
    When I put the caret at <caret>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1 <addition>:
        input:<content>
        wildcard_constraints:<statement_content>

    """
    When I put the caret at <caret>
    And I invoke MoveStatementDown action
    Then editor content will be
    """

    <rule_like> NAME1 <addition>:
        input:<content>
    wildcard_constraints:<statement_content>

    """
    Examples:
      | rule_like  | content              | statement_content | caret     | addition      |
      | rule       | "file.txt"           | wildcard="/d+1"   | wildcard= |               |
      | checkpoint | "file.txt"           | wildcard="/d+1"   | wildcard= |               |
      | use rule   | "file.txt"           | wildcard="/d+1"   | wildcard= | as NAME2 with |
      | rule       | \n        "file.txt" | wildcard="/d+1"   | wildcard= |               |
      | checkpoint | \n        "file.txt" | wildcard="/d+1"   | wildcard= |               |
      | use rule   | \n        "file.txt" | wildcard="/d+1"   | wildcard= | as NAME2 with |


  Scenario Outline: Move wildcard_constraints in/out rule (wildcard_constraints)
      Given a snakemake project
      Given I open a file "foo1.smk" with text
      """
      <rule_like> NAME1:
          input:<content>
      wildcard_constraints:
          <statement_content>
      """
      When I put the caret at <caret>
      And I invoke MoveStatementUp action
      Then editor content will be
      """
      <rule_like> NAME1:
          input:<content>
          wildcard_constraints:
              <statement_content>

      """
      When I put the caret at <caret>
      And I invoke MoveStatementUp action
      Then editor content will be
      """
      <rule_like> NAME1:
          wildcard_constraints:
              <statement_content>
          input:<content>

      """
      When I put the caret at <caret>
      And I invoke MoveStatementUp action
      Then editor content will be
      """
      wildcard_constraints:
          <statement_content>
      <rule_like> NAME1:
          input:<content>

      """
      When I put the caret at <caret>
      And I invoke MoveStatementDown action
      Then editor content will be
      """

      <rule_like> NAME1:
          wildcard_constraints:
              <statement_content>
          input:<content>

      """
      When I put the caret at <caret>
      And I invoke MoveStatementDown action
      Then editor content will be
      """

      <rule_like> NAME1:
          input:<content>
          wildcard_constraints:
              <statement_content>

      """
      When I put the caret at <caret>
      And I invoke MoveStatementDown action
      Then editor content will be
      """

      <rule_like> NAME1:
          input:<content>
      wildcard_constraints:
          <statement_content>

      """
      Examples:
        | rule_like   | content         | statement_content | caret |
        | rule        | "file.txt"      | wildcard="/d+1" |  wildcard= |
        | checkpoint  | "file.txt"      | wildcard="/d+1" |             wildcard= |
        | rule       | \n        "file.txt" | wildcard="/d+1" | wildcard= |
        | checkpoint | \n        "file.txt" | wildcard="/d+1" | wildcard= |


  Scenario Outline: Move section in/out rule doesn't work for rule with one section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1 <addition>:
        <statement_name>: <statement_content>
    """
    When I put the caret at <caret>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1 <addition>:
        <statement_name>: <statement_content>
    """
    When I put the caret at <caret>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1 <addition>:
        <statement_name>: <statement_content>
    """
    Examples:
      | rule_like   | statement_name       | statement_content         | caret                | addition      |
      | rule        | wildcard_constraints | wildcard="/d+1"           | wildcard=            |               |
      | checkpoint  | wildcard_constraints | wildcard="/d+1"           | wildcard_constraints |               |
      | subworkflow | workdir              | "/dir"                    | workdir              |               |
      | subworkflow | workdir              | "/dir"                    | "/dir"               |               |
      | module      | snakefile            | "/dir""                   | "/dir"               |               |
      | use rule    | wildcard_constraints | wildcard="/d+1"           | wildcard=            | as NAME2 with |
      | rule        | wildcard_constraints | \n        wildcard="/d+1" | wildcard=            |               |
      | checkpoint  | wildcard_constraints | \n        wildcard="/d+1" | wildcard_constraints |               |
      | subworkflow | workdir              | \n        "/dir"          | workdir              |               |
      | subworkflow | workdir              | \n        "/dir"          | "/dir"               |               |
      | module      | snakefile            | \n        "/dir"          | "/dir"               |               |
      | use rule    | wildcard_constraints | \n        wildcard="/d+1" | wildcard=            | as NAME2 with |

  Scenario Outline: Move section out rule doesn't work if destination is use section and moving section is execution section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule NAME:
      <section>:
        file="file.txt"

    use rule NAME2 as NAME3 with:
      threads: 5
    """
    When I put the caret at <section>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    rule NAME:
      <section>:
        file="file.txt"

    use rule NAME2 as NAME3 with:
      threads: 5
    """
    Examples:
      | section |
      | shell   |
      | run     |
      | wrapper |

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

  Scenario Outline: Move section inside rule (case1)
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME1:
        input: <content>
        <statement_name>: <statement_content>
    """
    When I put the caret at <statement_name>: <sign>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        input: <content>

    """
    When I put the caret at <statement_name>: <sign>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME1:
        <statement_name>: <statement_content>
        input: <content>

    """
    When I put the caret at ""
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        input: <content>
        <statement_name>: <statement_content>

    """
    When I put the caret at ""
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME1:
        input: <content>
        <statement_name>: <statement_content>

    """
    Examples:
      | rule_like | statement_name | content              | statement_content | sign  |
      | rule      | input          | "file.txt"           | ""                | ""    |
      | rule      | output         | "file.txt"           | ""                | ""    |
      | rule      | shell          | "file.txt"           | ""                | ""    |
      | rule      | log            | "file.txt"           | ""                | ""    |
      | rule      | params         | "file.txt"           | ""                | ""    |
      | rule      | resources      | "file.txt"           | ""                | ""    |
      | rule      | version        | "file.txt"           | ""                | ""    |
      | rule      | message        | "file.txt"           | ""                | ""    |
      | rule      | benchmark      | "file.txt"           | ""                | ""    |
      | rule      | priority       | "file.txt"           | ""                | ""    |
      | rule      | wrapper        | "file.txt"           | ""                | ""    |
      | rule      | input          | \n        "file.txt" | ""                | ""    |
      | rule      | output         | \n        "file.txt" | ""                | ""    |
      | rule      | shell          | \n        "file.txt" | ""                | ""    |
      | rule      | input          | "file.txt"           | #here\n        "" | #here |
      | rule      | output         | "file.txt"           | #here\n        "" | #here |
      | rule      | shell          | "file.txt"           | #here\n        "" | #here |
      | rule      | input          | \n        "file.txt" | #here\n        "" | #here |
      | rule      | output         | \n        "file.txt" | #here\n        "" | #here |
      | rule      | shell          | \n        "file.txt" | #here\n        "" | #here |

  Scenario Outline: Swap rule between rule sections
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like2> NAME2 <addition2>:
        input:<content2>
        output:<content2>

    # comment

    <rule_like1> NAME1 <addition1>:
        <section>:<content1>
        message:<content1>
    """
    When I put the caret at <section>:
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like2> NAME2 <addition2>:
        input:<content2>
        output:<content2>
        <section>:<content1>

    # comment

    <rule_like1> NAME1 <addition1>:
        message:<content1>
    """
    When I put the caret at <section>:
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like2> NAME2 <addition2>:
        input:<content2>
        output:<content2>

    # comment

    <rule_like1> NAME1 <addition1>:
        <section>:<content1>
        message:<content1>
    """
    Examples:
      | rule_like1 | rule_like2 | content1              | content2              | section         | addition1     | addition2     |
      | rule       | rule       | "file1.txt"           | "file2.txt"           | log             |               |               |
      | checkpoint | rule       | "file1.txt"           | "file2.txt"           | log             |               |               |
      | rule       | checkpoint | "file1.txt"           | "file2.txt"           | log             |               |               |
      | checkpoint | checkpoint | "file1.txt"           | "file2.txt"           | log             |               |               |
      | rule       | rule       | \n        "file1.txt" | "file2.txt"           | log             |               |               |
      | checkpoint | rule       | \n        "file1.txt" | "file2.txt"           | log             |               |               |
      | rule       | checkpoint | \n        "file1.txt" | "file2.txt"           | log             |               |               |
      | checkpoint | checkpoint | \n        "file1.txt" | "file2.txt"           | log             |               |               |
      | rule       | rule       | "file1.txt"           | \n        "file2.txt" | log             |               |               |
      | checkpoint | rule       | "file1.txt"           | \n        "file2.txt" | log             |               |               |
      | rule       | checkpoint | "file1.txt"           | \n        "file2.txt" | log             |               |               |
      | checkpoint | checkpoint | "file1.txt"           | \n        "file2.txt" | log             |               |               |
      | rule       | rule       | \n        "file1.txt" | \n        "file2.txt" | log             |               |               |
      | checkpoint | rule       | \n        "file1.txt" | \n        "file2.txt" | log             |               |               |
      | rule       | checkpoint | \n        "file1.txt" | \n        "file2.txt" | log             |               |               |
      | checkpoint | checkpoint | \n        "file1.txt" | \n        "file2.txt" | log             |               |               |
      | module     | module     | True                  | "file2.txt"           | skip_validation |               |               |
      | use rule   | use rule   | "file1.txt"           | "file2.txt"           | log             | as NAME3 with | as NAME4 with |
      | use rule   | rule       | "file1.txt"           | "file2.txt"           | log             | as NAME3 with |               |
      | rule       | use rule   | "file1.txt"           | "file2.txt"           | log             |               | as NAME4 with |
      | use rule   | checkpoint | "file1.txt"           | "file2.txt"           | log             | as NAME3 with |               |
      | checkpoint | use rule   | "file1.txt"           | "file2.txt"           | log             |               | as NAME4 with |

  Scenario Outline: Swap rule between rule sections doesn't work with content between them
     Given a snakemake project
     Given I open a file "foo1.smk" with text
     """
     <rule_like2> NAME2:
         input: <content2>
         output: <content2>

     pass

     <rule_like1> NAME1:
         message: <content1>
         log: <content1>
     """
     When I put the caret at input:
     And I invoke MoveStatementUp action
     Then editor content will be
     """
     <rule_like2> NAME2:
         input: <content2>
         output: <content2>

     pass

     <rule_like1> NAME1:
         message: <content1>
         log: <content1>
     """
     When I put the caret at output:
     And I invoke MoveStatementDown action
     Then editor content will be
     """
     <rule_like2> NAME2:
         input: <content2>
         output: <content2>

     pass

     <rule_like1> NAME1:
         message: <content1>
         log: <content1>
     """
     Examples:
       | rule_like1 | rule_like2 | content1              | content2              |
       | rule       | rule       | "file1.txt"           | "file2.txt"           |
       | checkpoint | rule       | "file1.txt"           | "file2.txt"           |
       | rule       | checkpoint | "file1.txt"           | "file2.txt"           |
       | checkpoint | checkpoint | "file1.txt"           | "file2.txt"           |
       | rule       | rule       | \n        "file1.txt" | "file2.txt"           |
       | checkpoint | rule       | \n        "file1.txt" | "file2.txt"           |
       | rule       | checkpoint | \n        "file1.txt" | "file2.txt"           |
       | checkpoint | checkpoint | \n        "file1.txt" | "file2.txt"           |
       | rule       | rule       | "file1.txt"           | \n        "file2.txt" |
       | checkpoint | rule       | "file1.txt"           | \n        "file2.txt" |
       | rule       | checkpoint | "file1.txt"           | \n        "file2.txt" |
       | checkpoint | checkpoint | "file1.txt"           | \n        "file2.txt" |
       | rule       | rule       | \n        "file1.txt" | \n        "file2.txt" |
       | checkpoint | rule       | \n        "file1.txt" | \n        "file2.txt" |
       | rule       | checkpoint | \n        "file1.txt" | \n        "file2.txt" |
       | checkpoint | checkpoint | \n        "file1.txt" | \n        "file2.txt" |


  Scenario Outline: Swap statements in run section
      Given a snakemake project
      Given I open a file "foo1.smk" with text
      """
      <rule_like> NAME:
        run:
            <section_content1>
            <section_content2>
      """
      When I put the caret at <section_content2>
      And I invoke MoveStatementUp action
      Then editor content will be
      """
      <rule_like> NAME:
        run:
            <section_content2>
            <section_content1>

      """
      When I put the caret at <section_content2>
      And I invoke MoveStatementDown action
      Then editor content will be
      """
      <rule_like> NAME:
        run:
            <section_content1>
            <section_content2>

      """
      Examples:
        | rule_like  | section_content1 | section_content2 |
        | rule       | pass             | b: int           |
        | checkpoint | a: int           | b: int           |

  Scenario Outline: Swap python functions in run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like>
        run:
          def f1():
              <section1_content>
          def f2():
              <section2_content>
    """
    When I put the caret at def f2():
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like>
        run:
          def f2():
              <section2_content>
          def f1():
              <section1_content>
    """
    When I put the caret at def f2():
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like>
        run:
          def f1():
              <section1_content>
          def f2():
              <section2_content>
    """
    Examples:
      | rule_like  | section1_content | section2_content |
      | rule       | pass             | pass             |
      | checkpoint | a: int           | b: int           |

  Scenario Outline: Swap python function statements in run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    <rule_like> NAME:
      run:
          def f1():
              <section_content1>
              <section_content2>
    """
    When I put the caret at <section_content2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    <rule_like> NAME:
      run:
          def f1():
              <section_content2>
              <section_content1>

    """
    When I put the caret at <section_content2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    <rule_like> NAME:
      run:
          def f1():
              <section_content1>
              <section_content2>

    """
    Examples:
      | rule_like  | section_content1 | section_content2 |
      | rule       | pass             | b: int           |
      | checkpoint | a: int           | b: int           |

  Scenario Outline: Complex example for swap sections in python functions inside run section
    Given a snakemake project
    Given I open a file "foo1.smk" with text
    """
    rule NAME:
      run:
            def target(self, paths):
                <section1>
                <section2>
    """
    When I put the caret at <section2>
    And I invoke MoveStatementUp action
    Then editor content will be
    """
    rule NAME:
      run:
            def target(self, paths):
                <section2>
                <section1>

    """
    When I put the caret at <section2>
    And I invoke MoveStatementDown action
    Then editor content will be
    """
    rule NAME:
      run:
            def target(self, paths):
                <section1>
                <section2>

    """
    Examples:
      | section1  | section2    |
      | a: int    | b: int      |
      | s1 = "s"  | s2 = "s"    |

#  Bug 1: do not move single section (wildcards:)
#  rule NAME2:
#      input: ""
#  rule NAME1:
#      wildcard_constraints: wildcard="/d+1"
#
