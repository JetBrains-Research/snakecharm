Feature: Completion in python part of snakemake file

  Scenario: Complete any python method
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    foo = 1;
    """
    When I put the caret after foo = 1;
    And I invoke autocompletion popup
    Then completion list should contain:
    |print|
