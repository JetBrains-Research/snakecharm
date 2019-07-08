Feature: Spellchecker for snakemake exclusive psi elements

  #noinspection SpellCheckingInspection
  Scenario Outline: Check rule/checkpoint/subworkflow names
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> soooome:
        <section>: "in.txt"
    """
    And SpellCheckingInspection inspection is enabled
    And I expect inspection TYPO on <soooome> in <soooome:> with message
    """
    Typo: In word 'soooome'
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section   |
      | rule        | input     |
      | checkpoint  | input     |
      | subworkflow | snakefile |
