Feature: Inspection for duplicated arguments in same section
  Scenario Outline: Duplicated arguments in section
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
      | section              | rule_like   |
      | workdir              | subworkflow |
      | snakefile            | subworkflow |
      | configfile           | subworkflow |
      | input                | checkpoint  |
      | output               | checkpoint  |
      | params               | checkpoint  |
      | input                | rule        |
      | output               | rule        |
      | params               | rule        |
      | resources            | rule        |
      | log                  | rule        |
      | wildcard_constraints | rule        |

  Scenario Outline: SmkSectionDuplicatedArgsInspection element removal fix test
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
    Then I invoke quick fix Remove duplicated argument and see text:
    """
    <rule_like> NAME:
        <section>: "target_1", "target_2"  # duplicate
    """
    Examples:
      | section              | rule_like  |
      | configfile           | subworkflow |
      | input                | checkpoint  |
      | input                | rule        |
