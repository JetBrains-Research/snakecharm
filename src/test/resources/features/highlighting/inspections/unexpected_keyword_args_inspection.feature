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

  Scenario Outline: Unexpected keyword arguments in rule\checkpoint\module
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
      | rule_like  | section         |
      | rule       | benchmark       |
      | rule       | cache           |
      | rule       | conda           |
      | rule       | container       |
      | rule       | containerized   |
      | rule       | cwl             |
      | rule       | group           |
      | rule       | envmodules      |
      | rule       | singularity     |
      | rule       | threads         |
      | rule       | name            |
      | rule       | handover        |
      | checkpoint | message         |
      | checkpoint | notebook        |
      | checkpoint | priority        |
      | checkpoint | script          |
      | checkpoint | shadow          |
      | checkpoint | shell           |
      | checkpoint | version         |
      | checkpoint | wrapper         |
      | checkpoint | handover        |
      | module     | snakefile       |
      | module     | config          |
      | module     | skip_validation |
      | module     | meta_wrapper    |


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

  Scenario Outline: Unexpected keyword arguments in workflow
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section_name>: a="foo.bar"
    """
    And SmkSectionUnexpectedKeywordArgsInspection inspection is enabled
    Then I expect inspection error on <a="foo.bar"> with message
    """
    Section '<section_name>' does not support keyword arguments
    """
    When I check highlighting errors
    Examples:
      | section_name  |
      | containerized |
      | singularity   |
      | container     |
