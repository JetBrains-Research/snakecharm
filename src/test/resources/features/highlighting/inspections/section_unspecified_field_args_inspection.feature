Feature: Inspection for arguments accessed via rules with unspecified field

  Scenario Outline: Identify rule dependency without field access
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: rules.RULE
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect inspection error on <rules.RULE> with message
    """
    No field from 'rules.RULE' is specified
    """
    When I check highlighting errors
    Examples:
      | rule_like   | section    |
      | rule        | input      |
      | rule        | output     |
      | rule        | params     |
      | rule        | resources  |
      | checkpoint  | shell      |
      | checkpoint  | script     |
      | checkpoint  | threads    |
      | checkpoint  | priority   |
      | subworkflow | workdir    |
      | subworkflow | snakefile  |
      | subworkflow | configfile |

  Scenario Outline: Identify rule dependency without field access as subexpression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: rules.RULE <addition>
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect inspection error on <rules.RULE> with message
    """
    No field from 'rules.RULE' is specified
    """
    When I check highlighting errors
    Examples:
      | rule_like  | section  | addition         |
      | rule       | input    | + ".bam"         |
      | rule       | output   | + ".bam"         |
      | rule       | params   | + "-O3"          |
      | checkpoint | shell    | + " > /dev/null" |
      | checkpoint | threads  | + 8              |
      | checkpoint | priority | + 10             |

  Scenario Outline: No errors on rule dependency with field access
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: rules.RULE.<section>
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like   | section    |
      | rule        | input      |
      | rule        | output     |
      | rule        | params     |
      | rule        | resources  |
      | checkpoint  | shell      |
      | checkpoint  | script     |
      | checkpoint  | threads    |
      | checkpoint  | priority   |
      | subworkflow | workdir    |
      | subworkflow | snakefile  |
      | subworkflow | configfile |

  Scenario Outline: No errors on rule dependency with field access as subexpression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: rules.RULE.<section> <addition>
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  | section   | addition         |
      | rule       | input     | + ".bam"         |
      | rule       | output    | + ".bam"         |
      | rule       | params    | + ".bam"         |
      | rule       | resources | + "-O3"          |
      | checkpoint | shell     | + " > /dev/null" |
      | checkpoint | threads   | + 8              |
      | checkpoint | priority  | + 10             |
