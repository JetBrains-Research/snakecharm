Feature: Resolve workflow file names to their corresponding declaration

  Scenario Outline: Resolve to a file in the same directory
    Given a snakemake project
    Given a file "boo.smk" with text
    """
    rule NAME:
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "boo.smk"
    """
    When I put the caret at boo
    Then reference should resolve to "rule" in "boo.smk"
    Examples:
      | workflow   |
      | include    |
      | configfile |
      | report     |

  Scenario Outline: Resolve to a file in a sub directory
    Given a snakemake project
    Given a file "A/boo.smk" with text
    """
    rule NAME:
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "A/boo.smk"
    """
    When I put the caret at boo
    Then reference should resolve to "rule" in "boo.smk"
    Examples:
      | workflow   |
      | include    |
      | configfile |
      | report     |