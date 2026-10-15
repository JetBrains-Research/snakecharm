Feature: 'pathvars' directive (Snakemake >= 9.12.0)
  See https://snakemake.readthedocs.io/en/stable/snakefiles/rules.html#snakefiles-pathvars

  Scenario: No errors on 'pathvars' in top-level, rule, checkpoint, use and module
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    pathvars:
        per="{sample}",
        results="custom/results"

    rule a:
        input:
            "<results>/something/<per>.txt"
        output:
            "<results>/processed/<per>.txt"
        pathvars:
            results="custom-folder"
        shell:
            "somecommand {input} {output}"

    checkpoint b:
        output:
            "<results>/b.txt"
        pathvars:
            results="b-folder"
        shell:
            "touch {output}"

    use rule a as c with:
        pathvars:
            results="other-folder",
            per="{other}"

    module other:
        snakefile: "other/Snakefile"
        pathvars:
            results="results/other"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    And SmkSectionMultipleArgsInspection inspection is enabled
    And SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors

  Scenario: 'pathvars' sections are recognized
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule a:
        output:
            "<results>/processed.txt"
        pathvars:
            results="custom-folder"
        shell:
            "touch {output}"

    use rule a as c with:
        pathvars:
            results="other-folder"

    module other:
        snakefile: "other/Snakefile"
        pathvars:
            results="results/other"
    """
    And SmkUnrecognizedSectionInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings

  Scenario: Top-level 'pathvars' directive in old language level
    Given a snakemake project
    And I set snakemake language version to "9.11.0"
    Given I open a file "foo.smk" with text
    """
    pathvars:
        results="custom/results"
    """
    And SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <pathvars> with message
    """
    Top level directive 'pathvars' was added in version 9.12.0, but selected Snakemake version is 9.11.0
    """
    When I check highlighting errors

  Scenario Outline: Subsection 'pathvars' in old language level
    Given a snakemake project
    And I set snakemake language version to "9.11.0"
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
        pathvars: results="custom/results"
    """
    And SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <pathvars> with message
    """
    Usage of 'pathvars' in '<rule_like>' was added in version 9.12.0, but selected Snakemake version is 9.11.0
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |
      | module     |
