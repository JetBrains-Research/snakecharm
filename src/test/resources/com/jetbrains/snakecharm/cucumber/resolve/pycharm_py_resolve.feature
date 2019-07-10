Feature: PyCharm resolve in snakemake file
  Could be affected by Snakemake PSI

  Scenario Outline: Resolve at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      foo = 1
      foo # here1

      rule name:
         input: "in.txt"

      boo = 2
      boo # here
      foo # here2
      """
    When I put the caret at <ptn>
    Then reference should resolve to "<item>" in "<file>"
    Examples:
      | ptn         | item | file    |
      | foo # here1 | foo  | foo.smk |
      | foo # here2 | foo  | foo.smk |
      | boo # here  | boo  | foo.smk |

  Scenario Outline: Resolve in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name:
       run:
          foo = 1
          boo = 2
          <text>
    """
    When I put the caret at <text>
    Then reference should resolve to "<item>" in "<file>"
    Examples:
      | rule_like  | text       | item | file    |
      | rule       | foo # here | foo  | foo.smk |
      | rule       | boo # here | boo  | foo.smk |
      | checkpoint | boo # here | boo  | foo.smk |

  Scenario Outline: Resolve in workflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <block> name:
          foo = 1
          boo = 2
          <text>
      """
    When I put the caret at <text>
    Then reference should resolve to "<item>" in "<file>"
    Examples:
      | block     | text       | item | file    |
      | onstart   | foo # here | foo  | foo.smk |
      | onstart   | boo # here | boo  | foo.smk |
      | onerror   | boo # here | boo  | foo.smk |
      | onsuccess | boo # here | boo  | foo.smk |