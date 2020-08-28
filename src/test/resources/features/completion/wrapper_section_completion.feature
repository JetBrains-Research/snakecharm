Feature: Completion for wrapper name

  Scenario Outline: Complete wrapper name for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "0.64.0/<short_name>"
    """
    When I put the caret after <short_name>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <full_name> |
    Examples:
      | rule_like  | short_name         | full_name                       |
      | rule       | bismark2report     | bio/bismark/bismark2report      |
      | rule       | fastqc             | bio/fastqc                      |
      | checkpoint | cairosvg           | utils/cairosvg                  |
      | checkpoint | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |
