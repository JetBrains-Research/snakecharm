Feature: Inspection for duplicated arguments in same section
  Scenario Outline: Duplicated arguments in execution sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: "target_1", "target_2", "target_2" # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <"target_2"> in <"target_2" # duplicate> with message
    """
    This argument has already been added to '<section>' section.
    """
    When I check highlighting warnings
    Examples:
      | section              | rule_like  |
      | input                | checkpoint |
      | input                | rule       |
      | output               | rule       |
      | params               | rule       |
      | resources            | rule       |
      | log                  | rule       |
      | wildcard_constraints | rule       |

  Scenario Outline: Duplicated arguments rename fix test
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
        <section>: "target_1", "target_2", "target_2" # duplicate
    """
    And SmkSectionDuplicatedArgsInspection inspection is enabled
    Then I expect inspection warning on <"target_2"> in <"target_2" # duplicate> with message
    """
    This argument has already been added to '<section>' section.
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove duplicated element and see text:
    """
    <rule_like> NAME:
        <section>: "target_1", "target_2"  # duplicate
    """
    Examples:
      | section              | rule_like  |
      | input                | checkpoint |
      | input                | rule       |
