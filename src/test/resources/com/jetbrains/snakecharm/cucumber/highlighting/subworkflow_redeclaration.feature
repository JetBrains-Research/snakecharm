Feature: Subworkflow redeclaration inspection
  Scenario: A single subworkflow redeclaration
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        snakefile: "snake.smk"

    subworkflow ANOTHER_NAME:
        snakefile: "foo.smk"

    subworkflow NAME: #overrides
        snakefile: "boo.smk"
    """
    And Subworkflow Redeclaration inspection is enabled
    Then I expect inspection weak warning on <NAME> in <subworkflow NAME:> with message
    """
    Only last subworkflow with the same name will be executed
    """
    When I check highlighting weak warnings