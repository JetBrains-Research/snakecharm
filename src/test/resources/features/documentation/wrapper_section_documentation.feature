Feature: Documentation for wrapper section

  Scenario Outline: Documentation links for default settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      wrapper: '<example>'
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret at <example>
    And I invoke quick documentation popup
    Then Documentation text should contain a substring: <doc_link>
    And Documentation text should contain a substring: <src_link>
    Examples:
      | example                                                                     | doc_link                                                                             | src_link                                                                           |
      | 0.50.0/bio/bismark/bismark                                                   | https://snakemake-wrappers.readthedocs.io/en/0.50.0/wrappers/bismark/bismark.html    | https://github.com/snakemake/snakemake-wrappers/tree/0.50.0/bio/bismark/bismark    |
      | v4.6.0/bio/bwa/mem                                                           | https://snakemake-wrappers.readthedocs.io/en/v4.6.0/wrappers/bwa/mem.html            | https://github.com/snakemake/snakemake-wrappers/tree/v4.6.0/bio/bwa/mem            |
      | v5.0.0/bio/bwa/mem                                                           | https://snakemake-wrappers.readthedocs.io/en/v5.0.0/wrappers/bwa/mem.html            | https://github.com/snakemake/snakemake-wrappers/tree/v5.0.0/bio/bwa/mem            |
      | v6.0.0/bio/bwa/mem                                                           | https://snakemake-wrappers.readthedocs.io/en/v6.0.0/wrappers/bio/bwa/mem.html        | https://github.com/snakemake/snakemake-wrappers/tree/v6.0.0/bio/bwa/mem            |
      | v7.2.0/bio/bwa/mem                                                           | https://snakemake-wrappers.readthedocs.io/en/v7.2.0/wrappers/bio/bwa/mem.html        | https://github.com/snakemake/snakemake-wrappers/tree/v7.2.0/bio/bwa/mem            |
      | v4.6.0/bio/bwa/mem-samblaster                                                | https://snakemake-wrappers.readthedocs.io/en/v4.6.0/wrappers/bwa/mem-samblaster.html | https://github.com/snakemake/snakemake-wrappers/tree/v4.6.0/bio/bwa/mem-samblaster |
      | 0.62.0/bio/hisat2/index                                                      | https://snakemake-wrappers.readthedocs.io/en/0.62.0/wrappers/hisat2/index.html       | https://github.com/snakemake/snakemake-wrappers/tree/0.62.0/bio/hisat2/index       |
      | 0.63.0/bio/samtools/sort                                                     | https://snakemake-wrappers.readthedocs.io/en/0.63.0/wrappers/samtools/sort.html      | https://github.com/snakemake/snakemake-wrappers/tree/0.63.0/bio/samtools/sort      |
      | v6.0.0/phys/root/rootcp                                                      | https://snakemake-wrappers.readthedocs.io/en/v6.0.0/wrappers/phys/root/rootcp.html   | https://github.com/snakemake/snakemake-wrappers/tree/v6.0.0/phys/root/rootcp       |
      | v7.0.0/geo/pygadm/item                                                       | https://snakemake-wrappers.readthedocs.io/en/v7.0.0/wrappers/geo/pygadm/item.html    | https://github.com/snakemake/snakemake-wrappers/tree/v7.0.0/geo/pygadm/item        |
      | v7.0.0/utils/nextflow                                                        | https://snakemake-wrappers.readthedocs.io/en/v7.0.0/wrappers/utils/nextflow.html     | https://github.com/snakemake/snakemake-wrappers/tree/v7.0.0/utils/nextflow         |
      | master/bio/multiqc                                                           | https://snakemake-wrappers.readthedocs.io/en/latest/wrappers/multiqc.html            | https://github.com/snakemake/snakemake-wrappers/tree/master/bio/multiqc            |
      | latest/bio/arriba                                                            | https://snakemake-wrappers.readthedocs.io/en/latest/wrappers/arriba.html             | https://github.com/snakemake/snakemake-wrappers/tree/master/bio/arriba             |
      | https://github.com/snakemake/snakemake-wrappers/raw/0.2.0/bio/samtools/sort  | https://snakemake-wrappers.readthedocs.io/en/0.2.0/wrappers/samtools/sort.html       | https://github.com/snakemake/snakemake-wrappers/raw/0.2.0/bio/samtools/sort        |
      | https://github.com/snakemake/snakemake-wrappers/raw/v2.0.0/bio/samtools/sort | https://snakemake-wrappers.readthedocs.io/en/v2.0.0/wrappers/samtools/sort.html      | https://github.com/snakemake/snakemake-wrappers/raw/v2.0.0/bio/samtools/sort       |
      | https://github.com/snakemake/snakemake-wrappers/raw/v6.0.0/bio/samtools/sort | https://snakemake-wrappers.readthedocs.io/en/v6.0.0/wrappers/bio/samtools/sort.html  | https://github.com/snakemake/snakemake-wrappers/raw/v6.0.0/bio/samtools/sort       |
      | https://my_storage/0.2.0/bio/samtools/sort                                   | Unknow documentation and source link                                                 | Unknow documentation and source link                                               |

  Scenario Outline: Documentation sections from wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      wrapper: <quote><example><quote>
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret at <example>
    And I invoke quick documentation popup
    And Documentation text should contain a substring: <name>
    And Documentation text should contain a substring: <description>
    And Documentation text should contain a substring: <authors>
    Examples:
      | quote | example                       | name                 | authors      | description                   |
      | '     | 0.50.0/bio/bismark/bismark    | bismark              | Cherniatchik | BS-Seq reads using Bismark    |
      | "     | v4.6.0/bio/bwa/mem            | "bwa mem"            | Köster       | Map reads using bwa mem, with |
      | "     | v4.6.0/bio/bwa/mem-samblaster | "bwa mem samblaster" | Schröder     | mark duplicates by samblaster |
      | "     | 0.62.0/bio/hisat2/index       | hisat2 index         | Simoneau     | index with hisat2             |
      | """   | 0.63.0/bio/samtools/sort      | samtools sort        | Köster       | Sort bam file with samtools   |
      | "     | master/bio/multiqc            | multiqc              | Ruiter       | qc report using multiqc       |
      | '     | latest/bio/arriba             | name                 | authors      | gene fusions                  |

  Scenario Outline: Documentation text from wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      wrapper: '<example>'
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret at <example>
    And I invoke quick documentation popup
    And Documentation text should contain substrings:
      | name:        |
      | description: |
      | url:         |
      | authors:     |
      | input:       |
      | output:      |
      | notes:       |

    Examples:
      | example            |
      | v4.6.0/bio/bwa/mem |