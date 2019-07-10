Feature: PyCharm auto-completion in snakemake file
  Could be affected by Snakemake PSI

  Scenario Outline: Complete at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      foo = 1
      foo # here

      rule name:
         input: "in.txt"

      boo = 2
      boo # here
      """
    When I put the caret at <ptn>
    And I invoke autocompletion popup
    Then completion list should contain items <item>
    Examples:
      | ptn        | item     |
      | foo # here | foo      |
      | boo # here | foo, boo |

  Scenario Outline: Complete in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> name:
       run:
          foo = 1
          boo = 2
          foo # here
    """
    When I put the caret at foo # here
    And I invoke autocompletion popup
    Then completion list should contain items foo, boo
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Complete in workflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      <block> name:
          foo = 1
          boo = 2
          foo # here
      """
    When I put the caret at foo # here
    And I invoke autocompletion popup
    Then completion list should contain items foo, boo
    Examples:
      | block     |
      | onstart   |
      | onerror   |
      | onsuccess |