Feature: Documentation for wrapper section
  Scenario Outline: Documentation for default settings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      wrapper: <quote><example><quote>
    """
    When I put the caret at <example>
    And I invoke quick documentation popup
    Then Documentation text should contain <documentation>
    Examples:
      | quote | example                    | documentation                                                                     |
      | '     | 0.50.0/bio/bismark/bismark | https://snakemake-wrappers.readthedocs.io/en/0.50.0/wrappers/bismark/bismark.html |
      | "     | 0.62.0/bio/hisat2/index    | https://snakemake-wrappers.readthedocs.io/en/0.62.0/wrappers/hisat2/index.html    |
      | """   | 0.63.0/bio/samtools/sort   | https://snakemake-wrappers.readthedocs.io/en/0.63.0/wrappers/samtools/sort.html   |
