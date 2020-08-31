Feature: Completion for wrapper name

  Scenario Outline: Complete wrapper name for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "0.64.0/<short_name>"
    """
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "0.64.0/<full_name>"
    """
    Examples:
      | rule_like  | short_name         | full_name                       |
      | rule       | bismark2report     | bio/bismark/bismark2report      |
      | rule       | fastqc             | bio/fastqc                      |
      | checkpoint | cairosvg           | utils/cairosvg                  |
      | checkpoint | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |

  Scenario Outline: Complete wrapper name for non-standard version tag
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<tag>/<short_name>"
    """
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<tag>/<full_name>"
    """
    Examples:
      | rule_like  | tag     | short_name         | full_name                       |
      | rule       | master  | bismark2report     | bio/bismark/bismark2report      |
      | rule       | latest  | fastqc             | bio/fastqc                      |
      | checkpoint | 0.54.3  | cairosvg           | utils/cairosvg                  |
      | checkpoint | 0.30.10 | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |
