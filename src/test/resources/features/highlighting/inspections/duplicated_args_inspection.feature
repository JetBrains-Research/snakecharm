Feature: Inspection for duplicated arguments in same section
  Scenario Outline: Duplicated string arguments in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: "target_1", "target_2", "target_2" # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <"target_2"> in <"target_2" # duplicate> with message
    """
    This argument has been already added to '<section>' section.
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section              |
      | subworkflow | workdir              |
      | subworkflow | snakefile            |
      | subworkflow | configfile           |
      | checkpoint  | input                |
      | checkpoint  | output               |
      | checkpoint  | params               |
      | rule        | input                |
      | rule        | output               |
      | rule        | params               |
      | rule        | resources            |
      | rule        | log                  |
      | rule        | wildcard_constraints |

  Scenario Outline: Duplicated method call arguments in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    T=["A", "B"]
    <rule_like> NAME:
        <section>: expand("{t}1", t=T), expand("{t}2", t=T), expand("{t}2", t=T) # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <expand("{t}2", t=T)> in <expand("{t}2", t=T) # duplicate> with message
    """
    This argument has been already added to '<section>' section.
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section              |
      | subworkflow | workdir              |
      | subworkflow | snakefile            |
      | subworkflow | configfile           |
      | checkpoint  | input                |
      | checkpoint  | output               |
      | checkpoint  | params               |
      | rule        | input                |
      | rule        | output               |
      | rule        | params               |
      | rule        | resources            |
      | rule        | log                  |
      | rule        | wildcard_constraints |

  Scenario Outline: Duplicated string with wildcard arguments in section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: "{sample}.txt", "{sample}.bin", "{sample}.bin" # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <"{sample}.bin"> in <"{sample}.bin" # duplicate> with message
    """
    This argument has been already added to '<section>' section.
    """
    When I check highlighting warnings
    Examples:
      | rule_like   | section              |
      | subworkflow | workdir              |
      | subworkflow | snakefile            |
      | subworkflow | configfile           |
      | checkpoint  | input                |
      | checkpoint  | output               |
      | checkpoint  | params               |
      | rule        | input                |
      | rule        | output               |
      | rule        | params               |
      | rule        | resources            |
      | rule        | log                  |
      | rule        | wildcard_constraints |

  Scenario Outline: Duplicated keyword arguments in section ()
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: a="foo", b="bar", b="bar" # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings ignoring extra highlighting
    Examples:
      | rule_like   | section              |
      | subworkflow | workdir              |
      | subworkflow | snakefile            |
      | subworkflow | configfile           |
      | checkpoint  | input                |
      | checkpoint  | output               |
      | checkpoint  | params               |
      | rule        | input                |
      | rule        | output               |
      | rule        | params               |
      | rule        | resources            |
      | rule        | log                  |
      | rule        | wildcard_constraints |

  Scenario Outline: SmkSectionDuplicatedArgsInspection element removal fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: <arg1>, <arg2>, <arg2> # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <<arg2>> in <<arg2> # duplicate> with message
    """
    This argument has been already added to '<section>' section.
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove duplicated argument and see text:
    """
    <rule_like> NAME:
        <section>: <arg1>, <arg2>  # duplicate
    """
    Examples:
      | rule_like   | section    | arg1                         | arg2                         |
      | subworkflow | configfile | "target_1"                   | "target_2"                   |
      | checkpoint  | input      | "{sample}.txt"               | "{sample}.bin"               |
      | rule        | output     | expand("{t}1", t=["A", "B"]) | expand("{t}2", t=["A", "B"]) |
