Feature: Inspection for unexpected keyword arguments in section

  Scenario Outline: Unexpected keyword arguments in subworkflow
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section>' does not support keyword arguments
    """
    When I check highlighting errors
    Examples:
      | section    |
      | workdir    |
      | snakefile  |
      | configfile |

  Scenario Outline: Unexpected keyword arguments in rule\checkpoint
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section>' does not support keyword arguments
    """
    When I check highlighting errors
    Examples:
      | rule_like  | section     |
      | rule       | benchmark   |
      | rule       | cache       |
      | rule       | conda       |
      | rule       | container   |
      | rule       | cwl         |
      | rule       | group       |
      | rule       | singularity |
      | rule       | threads     |
      | checkpoint | message     |
      | checkpoint | notebook    |
      | checkpoint | priority    |
      | checkpoint | script      |
      | checkpoint | shadow      |
      | checkpoint | shell       |
      | checkpoint | version     |
      | checkpoint | wrapper     |

  Scenario Outline: No warn on expected keyword arguments in rule\checkpoint
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section              |
      | rule       | input                |
      | rule       | output               |
      | rule       | params               |
      | checkpoint | log                  |
      | checkpoint | resources            |
      | checkpoint | wildcard_constraints |
