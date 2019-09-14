Feature: Inspection for missing 'wildcards.' prefix

  Scenario Outline: Wildcards prefix is missing
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section1>: "{sample}.txt"
      <section2>: "{sample}.log"
    """
    And SmkSLMissingWildcardsAccessorPrefixInspection inspection is enabled
    Then I expect inspection warning on <sample> in <{sample}.log> with message
    """
    Wildcard access requires 'wildcards.' prefix in this section.
    """
    When I check highlighting warnings
    And I invoke quick fix Add 'wildcards.' prefix. and see text:
    """
    <rule_like> NAME:
      <section1>: "{sample}.txt"
      <section2>: "{wildcards.sample}.log"
    """
    Examples:
      | rule_like  | section1 | section2 |
      | rule       | output   | shell    |
      | rule       | output   | message  |
      | rule       | input    | shell    |
      | rule       | params   | shell    |
      | checkpoint | output   | shell    |

  Scenario Outline: Wildcards like name is resolved into other place
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    sample = 1
    <rule_like> NAME:
      <section1>: "{sample}.txt"
      <section2>: "{sample}.log"
    """
    And SmkSLMissingWildcardsAccessorPrefixInspection inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | section1 | section2 |
      | rule       | output   | params   |
      | rule       | output   | input    |
      | rule       | input    | params   |
      | rule       | input    | log      |
      | checkpoint | output   | params   |

  Scenario Outline: Wildcards prefix isn't required
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section1>: "{sample}.txt"
      <section2>: "{sample}.log"
    """
    And SmkSLMissingWildcardsAccessorPrefixInspection inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | section1 | section2 |
      | rule       | output   | params   |
      | rule       | output   | input    |
      | rule       | input    | params   |
      | rule       | input    | log      |
      | checkpoint | output   | params   |
