Feature: Documentation for wrapper section

  Scenario Outline: Documentation for default settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      wrapper: <quote><example><quote>
    """
    And add snakemake facet with wrappers loaded
    When I put the caret at <example>
    And I invoke quick documentation popup
    Then Documentation text should contain <doc_link>
    And Documentation text should contain <src_link>
    And Documentation text should contain name
    And Documentation text should contain description
    And Documentation text should contain authors
    Examples:
      | quote | example                    | doc_link                                                                          | src_link                                                                        |
      | '     | 0.50.0/bio/bismark/bismark | https://snakemake-wrappers.readthedocs.io/en/0.50.0/wrappers/bismark/bismark.html | https://github.com/snakemake/snakemake-wrappers/tree/0.50.0/bio/bismark/bismark |
      | "     | 0.62.0/bio/hisat2/index    | https://snakemake-wrappers.readthedocs.io/en/0.62.0/wrappers/hisat2/index.html    | https://github.com/snakemake/snakemake-wrappers/tree/0.62.0/bio/hisat2/index    |
      | """   | 0.63.0/bio/samtools/sort   | https://snakemake-wrappers.readthedocs.io/en/0.63.0/wrappers/samtools/sort.html   | https://github.com/snakemake/snakemake-wrappers/tree/0.63.0/bio/samtools/sort   |
      | "     | master/bio/multiqc         | https://snakemake-wrappers.readthedocs.io/en/latest/wrappers/multiqc.html         | https://github.com/snakemake/snakemake-wrappers/tree/master/bio/multiqc         |
      | '     | latest/bio/arriba          | https://snakemake-wrappers.readthedocs.io/en/latest/wrappers/arriba.html          | https://github.com/snakemake/snakemake-wrappers/tree/master/bio/arriba          |