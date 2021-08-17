Feature: Completion pep object

  Scenario: Complete after pep object
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | config |