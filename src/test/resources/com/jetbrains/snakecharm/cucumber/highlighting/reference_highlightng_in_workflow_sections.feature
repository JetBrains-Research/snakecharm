Feature: This feature checks reference highlighting in workflow sections

  Scenario Outline: Highlight in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "boo.<file_type>"
    """
    When I put the caret after boo
    Then I expect reference highlighting on "boo.<file_type>"
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |

  Scenario: Highlight in 'workdir' section
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "Dir"
    """
    When I put the caret after Dir
    Then I expect reference highlighting on "Dir"

  Scenario Outline: Highlighting when string literal is divided
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "boo"".<file_type>"
    """
    When I put the caret at boo
    Then I expect reference highlighting on "boo"".<file_type>"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |

  Scenario: Highlighting in 'workdir' section when string literal is divided
    Given a snakemake project
    Given a directory "Dir"
    Given I open a file "foo.smk" with text
    """
    workdir: "D""ir"
    """
    When I put the caret after D
    Then I expect reference highlighting on "D""ir"

  Scenario Outline: Highlighting when string literal is crazily divided
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    """
    Given I open a file "foo.smk" with text
    """
    <workflow>: "" "b" 'oo' f"." f'<file_type>' ''
    """
    When I put the caret at b
    Then I expect reference highlighting on "" "b" 'oo' f"." f'<file_type>' '"
    Examples:
      | workflow   | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | report     | html      |