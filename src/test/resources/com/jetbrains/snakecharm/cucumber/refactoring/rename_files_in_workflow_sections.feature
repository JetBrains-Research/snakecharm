Feature: Rename files in workflow sections

  Scenario Outline: Rename in sections
    Given a snakemake project
    Given a file "boo.<file_type>" with text
    """
    rule NAME:
    """
    Given I open a file "foo.smk" with text
    """
    <section>: "boo.<file_type>"
    """
    When I put the caret after boo
    When I invoke rename with name "doo.<file_type>"
    Then the file "foo.smk" should have text
    """
    <section>: "doo.<file_type>"
    """
    And reference should resolve to "rule" in "doo.<file_type>"
    Examples:
      | section    | file_type |
      | include    | smk       |
      | configfile | yaml      |
      | configfile | yml       |
      | report     | html      |