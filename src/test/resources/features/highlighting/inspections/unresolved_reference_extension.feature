Feature: Inspection: SmkUnresolvedReferenceInspectionExtension
  Checks, that extension + inspection work

  Scenario Outline: Unresolved conda file
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     rule NAME:
         conda:
             "<path>"
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <<path>> with message
     """
     Unresolved reference '<path>'
     """
    When I check highlighting warnings
    And I invoke quick fix Create '<path>' and see text:
     """
     rule NAME:
         conda:
             "<path>"
     """
    Then the file "<path>" should have text
     """
     channels:
     dependencies:
     """
    Examples:
      | path              |
      | NAME.yaml         |
      | envs/NAME.yaml    |
      | ../envs/NAME.yaml |