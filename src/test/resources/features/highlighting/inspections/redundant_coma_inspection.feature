Feature: Inspection for redundant coma in the end of the line

  Scenario Outline: Redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt",
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt",> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | input   |

  Scenario Outline: Fix redundant coma in the end of the line
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      <section>: "input.txt",
    """
    And SmkRedundantComaInspection inspection is enabled
    Then I expect inspection warning on <,> in <<section>: "input.txt",> with message
    """
    Coma is unnecessary
    """
    When I check highlighting warnings
    Then I invoke quick fix Remove redundant coma and see text:
     """
    <rule_like> NAME:
      <section>: "input.txt"
    """
    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | params  |
      | checkpoint | input   |