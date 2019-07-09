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

  Scenario Outline: Resolve for multiple files
    Given a snakemake project
    Given a file "file1.smk" with text
    """
    rule NAME:
    """
    Given a file "file2.smk" with text
    """
    rule ANOTHER_NAME:
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "file1.smk", "file2.smk"
    """
    When I put the caret at file1
    Then reference should resolve to "rule" in "file1.smk"
    When I put the caret at file2
    Then reference should resolve to "rule" in "file2.smk"
    Examples:
      | workflow   |
      | include    |
      | configfile |
      | report     |