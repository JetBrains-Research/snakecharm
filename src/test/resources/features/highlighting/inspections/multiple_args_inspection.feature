Feature: Inspection for multiple arguments in various sections
  Scenario Outline: Multiple arguments in subworkflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for 'subworkflow' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for 'subworkflow' section.
    """
    When I check highlighting errors
    Examples:
    | section    |
    | workdir    |
    | snakefile  |
    | configfile |

  Scenario Outline: Multiple arguments in execution sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section.
    """
    When I check highlighting errors
    Examples:
      | section       |
      | shell         |
      | script        |
      | wrapper       |
      | cwl           |
      | conda         |
      | singularity   |
      | priority      |
      | version       |
      | cache         |
      | group         |
      | message       |
      | benchmark     |
      | threads       |
      | shadow        |
      | notebook      |
      | container     |
      | containerized |
      | handover      |

  Scenario Outline: Multiple arguments in workflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section_name>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section_name>' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section_name>' section.
    """
      When I check highlighting errors
    Examples:
      | section_name  |
      | containerized |
      | singularity   |
      | container     |
