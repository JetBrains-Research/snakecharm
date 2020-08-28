Feature: Inspection for missed wrapper arguments

  Scenario Outline: Missed argument in added section for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rulelike> foo:
      <section>: ""
      wrapper: "0.64.0/<wrapper>"
    """
    And SmkSectionWrapperArgsInspection inspection is enabled
    Then I expect inspection weak warning on <<section>> with message
    """
    Argument '<argument>' missed in '<section>'
    """
    When I check highlighting weak warnings ignoring extra highlighting
    Examples:
      | rulelike   | section   | wrapper                      | argument   |
      | rule       | input     | bio/fastp                    | sample     |
      | rule       | output    | bio/last/lastal              | maf        |
      | rule       | input     | bio/picard/bedtointervallist | bed        |
      | checkpoint | resources | bio/pear                     | mem_mb     |
      | checkpoint | params    | bio/vcftools/filter          | extra      |
      | checkpoint | input     | bio/gatk3/printreads         | recal_data |

  Scenario Outline: Missed section with arguments for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rulelike> foo:
      wrapper: "0.64.0/<wrapper>"
    """
    And SmkSectionWrapperArgsInspection inspection is enabled
    Then I expect inspection weak warning on <foo> with message
    """
    Section '<section>' with args '<arguments>' is missed
    """
    When I check highlighting weak warnings ignoring extra highlighting
    Examples:
      | rulelike   | section | wrapper                  | arguments          |
      | rule       | input   | bio/fastp                | sample             |
      | rule       | output  | bio/arriba               | fusions, discarded |
      | rule       | params  | bio/gatk3/printreads     | extra, java_opts   |
      | checkpoint | input   | bio/bedtools/coveragebed | a, b               |
      | checkpoint | input   | bio/pear                 | read1, read2       |
      | checkpoint | input   | bio/samtools/depth       | bams, bed          |
