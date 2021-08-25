Feature: Inspection: SmkWildcardInNotOverriddenSectionInspection

  Scenario Outline: Section contains wildcards, which no longer exist
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample}.txt"
      resources: "{sample}"

    use rule NAME as NAME2 with:
      output: "{sample2}.txt"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect inspection error on pattern <use rule NAME as NAME2 with:\n  output: "{sample2}.txt"> with message
    """
    Inherited section 'resources' contains undeclared wildcard 'sample'
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: No inspection if wildcard is valid
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample}.txt"
      resources: "{sample}"

    use rule NAME as NAME2 with:
      <section>: "{sample}.txt"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | section |
      | rule       | output  |
      | checkpoint | input   |