Feature: Inspection for arguments accessed via rules with unspecified field

  Scenario Outline: Identify rule dependency without field access
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <rule_like>s.RULE
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect inspection error on <<rule_like>s.RULE> with message
    """
    Expected section name or argument (e.g. 'rules.foo.input' or 'rules.bar.output.arg1'), but '<rule_like>s.RULE' was found
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

  Scenario Outline: Identify rule dependency without field access as subexpression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <rule_like>s.RULE <addition>
    """
    And SmkSectionUnspecifiedFieldArgsInspection inspection is enabled
    Then I expect inspection error on <<rule_like>s.RULE> with message
    """
    Expected section name or argument (e.g. 'rules.foo.input' or 'rules.bar.output.arg1'), but '<rule_like>s.RULE' was found
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
      <section>: <rule_like>s.RULE.<section>
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

  Scenario Outline: No errors on rule dependency with field access as subexpression
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: <rule_like>s.RULE.<section> <addition>
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
