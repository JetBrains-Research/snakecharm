Feature: PyCharm Control flow in snakemake file
  Could be affected by Snakemake PSI

  Scenario: Control flow at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      foo = 1
      foo
      """
    Then I expect controlflow
    """
    0(1) element: null
    1(2) element: PyAssignmentStatement
    2(3) WRITE ACCESS: foo
    3(4) element: PyExpressionStatement
    4(5) READ ACCESS: foo
    5() element: null
    """

  Scenario Outline: Control flow in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name:
       run:
          foo = 1
          foo
    """
    Then I expect controlflow
    """
    0(1) element: null
    1(2) element: <class>
    2(3) WRITE ACCESS: name
    3(4) element: PyAssignmentStatement
    4(5) WRITE ACCESS: foo
    5(6) element: PyExpressionStatement
    6(7) READ ACCESS: foo
    7() element: null
    """
    Examples:
      | rule_like  | class         |
      | rule       | SMKRule       |
      | checkpoint | SMKCheckPoint |

  Scenario Outline: Control flow in workflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <block> name:
          foo = 1
          foo
      """
    Then I expect controlflow
    """
    0(1) element: null
    1(2) element: PyTypeDeclarationStatement
    2(3) WRITE ACCESS: name
    3(4) element: PyAssignmentStatement
    4(5) WRITE ACCESS: foo
    5(6) element: PyExpressionStatement
    6(7) READ ACCESS: foo
    7() element: null
    """
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |