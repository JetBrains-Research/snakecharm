Feature: Spellchecker for snakemake exclusive psi elements

  #noinspection SpellCheckingInspection
  Scenario Outline: Check rule/checkpoint names
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

  Scenario: Check subworkflow names
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow soooome:
        snakefile: "in.txt"
    """
    And SpellCheckingInspection inspection is enabled
    And I expect inspection TYPO on <soooome> in <soooome:> with message
    """
    Typo: In word 'soooome'
    """
    And I expect inspection TYPO on <snakefile> in <snakefile:> with message
    """
    Typo: In word 'snakefile'
    """
    When I check highlighting warnings
