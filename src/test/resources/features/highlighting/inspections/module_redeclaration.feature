Feature: SmkModuleRedeclarationInspection inspection
  Scenario: A single SmkModuleRedeclarationInspection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    module NAME:
        snakefile: "snake.smk"

    module ANOTHER_NAME:
        snakefile: "foo.smk"

    module NAME: #overrides
        snakefile: "boo.smk"
    """
    And SmkModuleRedeclarationInspection inspection is enabled
    Then I expect inspection weak warning with message "Only last module with the same name will be executed" on
    """
    module NAME:
        snakefile: "snake.smk"
    """
    When I check highlighting weak warnings