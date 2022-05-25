Feature: Inspection for multiple arguments in various sections
  Scenario Outline: Multiple arguments in module/subworkflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <keyword> NAME:
        <section>: "a", "b", "c"
    """
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<keyword>' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<keyword>' section.
    """
    When I check highlighting errors
    Examples:
      | keyword     | section         |
      | subworkflow | workdir         |
      | subworkflow | snakefile       |
      | subworkflow | configfile      |
      | module      | snakefile       |
      | module      | config          |
      | module      | skip_validation |
      | module      | meta_wrapper    |

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
      | section        |
      | shell          |
      | script         |
      | wrapper        |
      | cwl            |
      | conda          |
      | singularity    |
      | priority       |
      | version        |
      | cache          |
      | group          |
      | message        |
      | benchmark      |
      | threads        |
      | shadow         |
      | notebook       |
      | container      |
      | containerized  |
      | handover       |
      | default_target |
      | retries        |

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
      | workdir       |
