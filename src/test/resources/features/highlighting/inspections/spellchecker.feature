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
    When I check highlighting warnings


  Scenario Outline: Do not warn about snakemake keywords and other known words
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: foo
    # soooome2
    """
    And SpellCheckingInspection inspection is enabled
    # ensure that test works, but section name not a typo
    And I expect inspection TYPO on <soooome> with message
    """
    Typo: In word 'soooome'
    """
    When I check highlighting warnings
    Examples:
      | section |
      | localrules |
      | ruleorder |
      | configfile |
      | workdir |
      | wildcard_constraints |
