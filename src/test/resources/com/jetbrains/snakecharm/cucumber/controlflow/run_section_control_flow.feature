Feature: PyCharm Control flow in snakemake file
  Could be affected by Snakemake PSI
  At the moment test just fixes current behavior, it isn't correct.

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
    3(4) element: SmkRunSection
    4(5) WRITE ACCESS: null
    5(6) element: PyAssignmentStatement
    6(7) WRITE ACCESS: foo
    7(8) element: PyExpressionStatement
    8(9) READ ACCESS: foo
    9() element: null
    """
    Examples:
      | rule_like  | class         |
      | rule       | SmkRule       |
      | checkpoint | SmkCheckPoint |

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
    1(2) element: SmkWorkflowPythonBlockSection
    2(3) WRITE ACCESS: null
    3(4) element: PyTypeDeclarationStatement
    4(5) WRITE ACCESS: name
    5(6) element: PyAssignmentStatement
    6(7) WRITE ACCESS: foo
    7(8) element: PyExpressionStatement
    8(9) READ ACCESS: foo
    9() element: null
    """
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |