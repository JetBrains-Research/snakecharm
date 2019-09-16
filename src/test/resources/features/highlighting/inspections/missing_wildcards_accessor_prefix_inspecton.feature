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

  Scenario Outline: Do not show warning in expand arguments
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: expand("{sample}.txt", sample="")
    """
    And SmkSLMissingWildcardsAccessorPrefixInspection inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario Outline: Do not show warning in lambda expression #249
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      output: "{prefix}"
      <section>: lambda wd: <text>
    """
    And I put the caret at prefix}.txt
    And I inject SmkSL at a caret
    Then I expect language injection on "{prefix}.txt"
    And SmkSLMissingWildcardsAccessorPrefixInspection inspection is enabled
    Then I expect no inspection warning
    When I check highlighting warnings
    Examples:
      | rule_like  | section | text                              |
      | rule       | input   | "{prefix}.txt"                    |
      | rule       | params  | ancient("{prefix}.txt")           |
      | rule       | input   | temp("{prefix}.txt")              |
      | rule       | input   | foo("{prefix}.txt")               |
      | checkpoint | input   | "{prefix}.txt"                    |
      | checkpoint | params  | ancient("{prefix}.txt")           |
      | checkpoint | input   | expand("{prefix}.txt", prefix="") |

