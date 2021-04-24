Feature: Inspection for missed wrapper arguments

  Scenario Outline: Missed argument in added section for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>: ""
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    And SmkWrapperMissedArgumentsInspection inspection is enabled
    Then I expect inspection weak warning on <<section>> with message
    """
    Argument '<argument>' missed in '<section>'
    """
    When I check highlighting weak warnings ignoring extra highlighting
    Examples:
      | rule_like  | section   | wrapper                      | argument   |
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
    <rule_like> foo:
      wrapper: "0.64.0/<wrapper>"
    """
     And add snakemake framework support with wrappers loaded
    And SmkWrapperMissedArgumentsInspection inspection is enabled
    Then I expect inspection weak warning on <foo> with message
    """
    Section '<section>' with args '<arguments>' is missed
    """
    When I check highlighting weak warnings ignoring extra highlighting
    Examples:
      | rule_like  | section | wrapper                  | arguments                                |
      | rule       | input   | bio/fastp                | sample                                   |
      | rule       | output  | bio/arriba               | discarded, fusions                       |
      | rule       | input   | bio/arriba               | annotation, bam                          |
      | rule       | params  | bio/arriba               | blacklist, extra, known_fusions, sv_file |
      | rule       | params  | bio/gatk3/printreads     | extra, java_opts                         |
      | checkpoint | input   | bio/bedtools/coveragebed | a, b                                     |
      | checkpoint | input   | bio/pear                 | read1, read2                             |
      | checkpoint | input   | bio/samtools/depth       | bams, bed                                |

  Scenario Outline: Missed section without arguments for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.64.0/<wrapper>"
    """
    And add snakemake framework support with wrappers loaded
    And SmkWrapperMissedArgumentsInspection inspection is enabled
    Then I expect inspection weak warning on <foo> with message
    """
    Section '<section>' is missed
    """
    When I check highlighting weak warnings ignoring extra highlighting
    Examples:
      | rule_like | section | wrapper           |
      | rule      | log     | bio/bcftools/call |
      | rule      | output  | bio/bcftools/call |
