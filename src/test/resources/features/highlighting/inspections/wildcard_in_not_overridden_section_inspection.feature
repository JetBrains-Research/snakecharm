Feature: Inspection: SmkWildcardInNotOverriddenSectionInspection

  Scenario Outline: Section contains wildcards, which no longer exist
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample}.txt"
      resources: "{sample}"

    use rule NAME as NAME2 with:
      output: "{sample2}.txt"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect inspection error on pattern <NAME2> with message
    """
    Inherited section 'resources' contains undeclared wildcard 'sample'
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: Section contains wildcards, which no longer exist v2
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> rule_418:
      input: "{sample}.in"
      output: "{sample}.out{sample2}"
      benchmark: "{sample}.out"
      shell: "touch {wildcards.sample2}"

    use rule rule_418 as rule_418b with:
      output:
        "{sample1}.out1"
      input: "{sample}.in"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect inspection error on pattern <rule_418b> with message
    """
    Inherited section 'shell' contains undeclared wildcard 'sample2'
    """
    When I check highlighting errors
    Examples:
      | section    |
      | rule       |
      | checkpoint |

  Scenario Outline: No inspection if wildcard is valid
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample}.txt"
      resources: "{sample}"

    use rule NAME as NAME2 with:
      <section>: "{sample}.txt"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | section |
      | rule       | output  |
      | checkpoint | input   |

  Scenario Outline: No inspection if section name injected
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      output: "{sample1}.txt"
      shell: "touch {output}"

    use rule NAME as NAME2 with:
      <section>: "{sample}.txt"
    """
    And SmkWildcardInNotOverriddenSectionInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | section    | section |
      | rule       | output  |
      | checkpoint | input   |