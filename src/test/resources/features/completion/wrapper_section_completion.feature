Feature: Completion for wrapper name

  Scenario Outline: Complete wrapper name with 0.64.0 version tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<short_name>"
    """
    When I put the caret after <short_name>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <full_name> |
    Examples:
      | rule_like  | short_name         | full_name                              |
      | rule       | bismark2report     | 0.64.0/bio/bismark/bismark2report      |
      | rule       | fastqc             | 0.64.0/bio/fastqc                      |
      | checkpoint | cairosvg           | 0.64.0/utils/cairosvg                  |
      | checkpoint | bam2fq/interleaved | 0.64.0/bio/samtools/bam2fq/interleaved |
