Feature: Color Settings Page

  Scenario: Checks that color settings page tags have the same highlighting with Smk annotators
    Given a snakemake project
    Given I open a color settings page text
    Then I expect the tag highlighting to be the same as the annotator highlighting
    When I check highlighting infos ignoring extra highlighting