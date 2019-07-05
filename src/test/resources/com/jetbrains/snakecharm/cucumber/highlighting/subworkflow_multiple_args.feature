Feature: Inspection for multiple arguments in 'subworkflow' sections
  Scenario Outline: Multiple settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: "a", "b"
    """
    And Subworkflow Multiple Args inspection is enabled
    Then I expect inspection error on <"a"> with message
    """
    Only one argument is allowed for subworkflow sections
    """
    And I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for subworkflow sections
    """
    When I check highlighting errors
    Examples:
    | section    |
    | workdir    |
    | snakefile  |
    | configfile |